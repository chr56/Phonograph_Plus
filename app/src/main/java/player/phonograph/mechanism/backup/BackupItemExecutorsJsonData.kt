/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.backup

import okio.Buffer
import okio.Source
import okio.buffer
import org.koin.core.context.GlobalContext
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.Song
import player.phonograph.model.backup.BackupItemExecutor
import player.phonograph.model.backup.ExportedFavorites
import player.phonograph.model.backup.ExportedInternalPlaylist
import player.phonograph.model.backup.ExportedInternalPlaylists
import player.phonograph.model.backup.ExportedPathFilter
import player.phonograph.model.backup.ExportedPlayingQueue
import player.phonograph.model.backup.ExportedPlaylist
import player.phonograph.model.backup.ExportedSetting
import player.phonograph.model.backup.ExportedSong
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.FavoriteSongs
import player.phonograph.repo.loader.PinedPlaylists
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.domain.RoomPlaylists
import player.phonograph.repo.room.domain.RoomPlaylistsActions
import player.phonograph.service.queue.MusicPlaybackQueueStore
import player.phonograph.service.queue.QueueManager
import player.phonograph.settings.Keys
import player.phonograph.settings.PathFilterSetting
import player.phonograph.settings.Setting
import player.phonograph.settings.SettingsDataSerializer
import androidx.datastore.preferences.core.edit
import android.content.Context
import kotlin.LazyThreadSafetyMode.NONE
import kotlinx.coroutines.flow.first
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException


sealed class JsonDataBackupItemExecutor : BackupItemExecutor {
    protected val format by lazy(NONE) {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    protected fun <T> write(context: Context, serializer: KSerializer<T>, item: T, name: String): Buffer? = try {
        val content = try {
            format.encodeToString(serializer, item)
        } catch (e: SerializationException) {
            warning(context, TAG, "Failed to serialize $name!", e)
            null
        }
        if (content != null && content.isNotEmpty()) {
            Buffer().apply { writeString(content, Charsets.UTF_8) }
        } else {
            warning(context, TAG, "No content for exporting $name!")
            null
        }
    } catch (e: IOException) {
        warning(context, TAG, "Failed to export $name!", e)
        null
    }

    protected fun <T> read(context: Context, serializer: KSerializer<T>, source: Source, name: String): T? = try {
        val rawString = source.buffer().use { bufferedSource -> bufferedSource.readUtf8() }
        try {
            format.decodeFromString(serializer, rawString)
        } catch (e: SerializationException) {
            warning(context, TAG, "Failed to read $name Backup!", e)
            null
        }
    } catch (e: IOException) {
        warning(context, TAG, "Failed to import $name!", e)
        null
    }

    companion object {
        private const val TAG = "BackupItemExecutor"
    }

}

object SettingsDataBackupItemExecutor : JsonDataBackupItemExecutor() {

    override suspend fun export(context: Context): Buffer? =
        try {
            val preferences = Setting(context).dataStore.data.first().asMap()
            val content = SettingsDataSerializer(format, context).serialize(preferences)
            Buffer().apply {
                writeString(content, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            warning(context, TAG, "Failed to convert SharedPreferences to Json", e)
            null
        }

    override suspend fun import(context: Context, source: Source): Boolean = try {

        val rawString = source.buffer().use { bufferedSource -> bufferedSource.readUtf8() }

        val rawJson = format.decodeFromString<ExportedSetting>(rawString)

        val json = rawJson.content
        if (rawJson.formatVersion < ExportedSetting.VERSION) {
            warning(context, TAG, "This file is using legacy format")
        }


        val content = try {
            SettingsDataSerializer(format, context).deserialize(json)
        } catch (e: SerializationException) {
            warning(context, TAG, "Failed to deserialize setting.", e)
            emptyArray()
        }
        Setting(context).dataStore.edit { preferences ->
            preferences.putAll(*content)
        }

        true
    } catch (e: Exception) {
        warning(context, TAG, "Failed to import Setting", e)
        false
    }

    private const val TAG = "SettingDataBackup"
}

// Deprecated; now for importing only.
object PathFilterDataBackupItemExecutor : JsonDataBackupItemExecutor() {
    override suspend fun export(context: Context): Buffer? {
        return null
    }

    override suspend fun import(context: Context, source: Source): Boolean {
        val imported = read(context, ExportedPathFilter.serializer(), source, "PathFilter")
        return if (imported != null) {
            PathFilterSetting.replace(context, true, imported.blacklist)
            PathFilterSetting.replace(context, false, imported.whitelist)
            true
        } else {
            false
        }
    }
}

object PlayingQueuesDataBackupItemExecutor : JsonDataBackupItemExecutor() {
    override suspend fun export(context: Context): Buffer? {
        val db = GlobalContext.get().get<MusicPlaybackQueueStore>()
        val originalPlayingQueue = db.savedOriginalPlayingQueue.map(::exportSong)
        val playingQueue = db.savedPlayingQueue.map(::exportSong)

        val exported = ExportedPlayingQueue(ExportedPlayingQueue.VERSION, playingQueue, originalPlayingQueue)
        return write(context, ExportedPlayingQueue.serializer(), exported, "PlayingQueues")
    }

    override suspend fun import(context: Context, source: Source): Boolean {
        val imported = read(context, ExportedPlayingQueue.serializer(), source, "PlayingQueues")

        return if (imported != null) {
            val db = GlobalContext.get().get<MusicPlaybackQueueStore>()
            val playingQueue = imported.playingQueue.mapNotNull { importSong(it, context) }
            val originalPlayingQueue = imported.originalPlayingQueue.mapNotNull { importSong(it, context) }
            synchronized(db) {
                db.saveQueues(playingQueue, originalPlayingQueue)
                GlobalContext.get().get<QueueManager>().reload()
            }
            true
        } else {
            false
        }
    }
}

object FavoritesDataBackupItemExecutor : JsonDataBackupItemExecutor() {
    override suspend fun export(context: Context): Buffer? {

        val songs = FavoriteSongs.all(context).map(::exportSong)

        val playlists = PinedPlaylists.all(context).map(::exportPlaylist)

        val exported = ExportedFavorites(ExportedFavorites.VERSION, songs, playlists)
        return write(context, ExportedFavorites.serializer(), exported, "Favorites")
    }

    override suspend fun import(context: Context, source: Source): Boolean {
        val imported = read(context, ExportedFavorites.serializer(), source, "Favorites")

        return if (imported != null) {

            val favoriteSongs = imported.favoriteSong.mapNotNull { importSong(it, context) }
            FavoriteSongs.clearAll(context)
            FavoriteSongs.add(context, favoriteSongs.asReversed())
            EventHub.sendEvent(context, EventHub.EVENT_FAVORITES_CHANGED)

            val pinedPlaylists = imported.pinedPlaylist.mapNotNull { importPlaylist(it, context) }
            PinedPlaylists.clearAll(context)
            PinedPlaylists.add(context, pinedPlaylists.asReversed())
            EventHub.sendEvent(context, EventHub.EVENT_PLAYLISTS_CHANGED)
            true
        } else {
            false
        }
    }

}

object InternalDatabasePlaylistsDataBackupItemExecutor : JsonDataBackupItemExecutor() {
    override suspend fun export(context: Context): Buffer? {
        val playlists =
            RoomPlaylists.all(context).map { playlist ->
                ExportedInternalPlaylist(
                    playlist.name,
                    RoomPlaylists.songs(context, playlist.location).map { exportSong(it.song) },
                    playlist.dateAdded,
                    playlist.dateModified
                )
            }
        val exported = ExportedInternalPlaylists(ExportedInternalPlaylists.VERSION, playlists)
        return write(context, ExportedInternalPlaylists.serializer(), exported, "InternalDatabasePlaylists")
    }

    override suspend fun import(context: Context, source: Source): Boolean {
        val imported = read(context, ExportedInternalPlaylists.serializer(), source, "InternalDatabasePlaylists")
        return if (imported != null) {
            for (playlist in imported.playlists) {
                RoomPlaylistsActions.import(
                    MusicDatabase.koinInstance,
                    playlist.name,
                    playlist.songs.mapNotNull { importSong(it, context) },
                    playlist.dateAdded,
                    playlist.dateModified,
                )
            }
            EventHub.sendEvent(context, EventHub.EVENT_PLAYLISTS_CHANGED)
            true
        } else {
            false
        }
    }
}

private fun exportSong(song: Song): ExportedSong =
    ExportedSong(song.data, song.title, song.albumName, song.artistName)

private suspend fun importSong(song: ExportedSong, context: Context): Song? =
    Songs.searchByPath(context, song.path, withoutPathFilter = true).firstOrNull()

private fun exportPlaylist(playlist: Playlist): ExportedPlaylist =
    ExportedPlaylist(playlist.path() ?: "-", playlist.name)

private suspend fun importPlaylist(playlist: ExportedPlaylist, context: Context): Playlist? =
    MediaStorePlaylists.searchByPath(context, playlist.path)