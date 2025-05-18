/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.backup

import lib.storage.textparser.ExternalFilePathParser
import okio.Buffer
import okio.Source
import okio.buffer
import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.foundation.reportError
import player.phonograph.foundation.warning
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
import player.phonograph.repo.database.store.FavoritesStore
import player.phonograph.repo.database.store.PathFilterStore
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.domain.PlaylistActions
import player.phonograph.repo.room.domain.RoomPlaylists
import player.phonograph.service.queue.MusicPlaybackQueueStore
import player.phonograph.service.queue.QueueManager
import player.phonograph.settings.Setting
import player.phonograph.settings.SettingsDataSerializer
import androidx.datastore.preferences.core.edit
import android.content.Context
import kotlin.LazyThreadSafetyMode.NONE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

    protected fun <T> write(serializer: KSerializer<T>, item: T, name: String): Buffer? = try {
        val content = try {
            format.encodeToString(serializer, item)
        } catch (e: SerializationException) {
            reportError(e, TAG, "Failed to serialize $name!")
            null
        }
        if (content != null && content.isNotEmpty()) {
            Buffer().apply { writeString(content, Charsets.UTF_8) }
        } else {
            warning(TAG, "No content for exporting $name!")
            null
        }
    } catch (e: IOException) {
        reportError(e, TAG, "Failed to export $name!")
        null
    }

    protected fun <T> read(serializer: KSerializer<T>, source: Source, name: String): T? = try {
        val rawString = source.buffer().use { bufferedSource -> bufferedSource.readUtf8() }
        try {
            format.decodeFromString(serializer, rawString)
        } catch (e: SerializationException) {
            reportError(e, TAG, "Failed to read $name Backup!")
            null
        }
    } catch (e: IOException) {
        reportError(e, TAG, "Failed to import $name!")
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
            reportError(e, TAG, "Failed to convert SharedPreferences to Json")
            null
        }

    override suspend fun import(context: Context, source: Source): Boolean = try {

        val rawString = source.buffer().use { bufferedSource -> bufferedSource.readUtf8() }

        val rawJson = format.decodeFromString<ExportedSetting>(rawString)

        val json = rawJson.content
        if (rawJson.formatVersion < ExportedSetting.VERSION) {
            warning(TAG, "This file is using legacy format")
        }


        val content = try {
            SettingsDataSerializer(format, context).deserialize(json)
        } catch (e: SerializationException) {
            reportError(e, TAG, "Failed to deserialize setting.")
            emptyArray()
        }
        Setting(context).dataStore.edit { preferences ->
            preferences.putAll(*content)
        }

        true
    } catch (e: Exception) {
        reportError(e, TAG, "Failed to import Setting")
        false
    }

    private const val TAG = "SettingDataBackup"
}

object PathFilterDataBackupItemExecutor : JsonDataBackupItemExecutor() {
    override suspend fun export(context: Context): Buffer? {
        val db = PathFilterStore.get()
        val whitelist = db.whitelistPaths
        val blacklist = db.blacklistPaths

        val exported = ExportedPathFilter(ExportedPathFilter.VERSION, whitelist, blacklist)
        return write(ExportedPathFilter.serializer(), exported, "PathFilter")
    }

    override suspend fun import(context: Context, source: Source): Boolean {
        val imported = read(ExportedPathFilter.serializer(), source, "PathFilter")
        return if (imported != null) {
            val db = PathFilterStore.get()
            synchronized(db) {
                db.clearBlacklist()
                db.addBlacklistPath(imported.blacklist)

                db.clearWhitelist()
                db.addWhitelistPath(imported.whitelist)
            }
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
        return write(ExportedPlayingQueue.serializer(), exported, "PlayingQueues")
    }

    override suspend fun import(context: Context, source: Source): Boolean {
        val imported = read(ExportedPlayingQueue.serializer(), source, "PlayingQueues")

        return if (imported != null) {
            val db = GlobalContext.get().get<MusicPlaybackQueueStore>()
            synchronized(db) {
                db.saveQueues(
                    imported.playingQueue.mapNotNull { importSong(it, context) },
                    imported.originalPlayingQueue.mapNotNull { importSong(it, context) },
                )
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
        val db = FavoritesStore.get()

        val songs =
            db.getAllSongs { _, path, _, _ -> lookupSong(context, path) }.map(::exportSong)

        val playlists =
            db.getAllPlaylists { id, path, _, _ -> lookupPlaylist(context, id, path) }.map(::exportPlaylist)


        val exported = ExportedFavorites(ExportedFavorites.VERSION, songs, playlists)
        return write(ExportedFavorites.serializer(), exported, "Favorites")
    }

    override suspend fun import(context: Context, source: Source): Boolean {
        val imported = read(ExportedFavorites.serializer(), source, "Favorites")

        return if (imported != null) {
            val db = FavoritesStore.get()
            synchronized(db) {
                val favoriteSong = imported.favoriteSong.mapNotNull { importSong(it, context) }
                db.clearAllSongs()
                db.addSongs(favoriteSong.asReversed())

                val pinedPlaylist = imported.pinedPlaylist.mapNotNull { importPlaylist(it, context) }
                db.clearAllPlaylists()
                db.addPlaylists(pinedPlaylist.asReversed())
            }
            EventHub.sendEvent(context, EventHub.EVENT_FAVORITES_CHANGED)
            EventHub.sendEvent(context, EventHub.EVENT_PLAYLISTS_CHANGED)
            true
        } else {
            false
        }
    }

    private suspend fun lookupSong(context: Context, path: String): Song {
        val song = Songs.path(context, path)
        return if (song == null) {
            val filename = ExternalFilePathParser.bashPath(path) ?: context.getString(R.string.deleted)
            Song.deleted(filename, path)
        } else {
            song
        }
    }

    private fun lookupPlaylist(context: Context, id: Long, path: String): Playlist? {

        val filePlaylist = MediaStorePlaylists.searchByPath(context, path)
        if (filePlaylist != null) return filePlaylist

        val databasePlaylist = null // Playlists.of(context, DatabasePlaylistLocation(path.toLongOrDefault(0)))
        @Suppress("SENSELESS_COMPARISON")
        if (databasePlaylist != null) return databasePlaylist

        return null
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
        return write(ExportedInternalPlaylists.serializer(), exported, "InternalDatabasePlaylists")
    }

    override suspend fun import(context: Context, source: Source): Boolean {
        val imported = read(ExportedInternalPlaylists.serializer(), source, "InternalDatabasePlaylists")
        return if (imported != null) {
            for (playlist in imported.playlists) {
                PlaylistActions.importPlaylist(
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

private fun importSong(song: ExportedSong, context: Context): Song? =
    runBlocking { Songs.searchByPath(context, song.path, withoutPathFilter = true).firstOrNull() }

private fun exportPlaylist(playlist: Playlist): ExportedPlaylist =
    ExportedPlaylist(playlist.path() ?: "-", playlist.name)

private fun importPlaylist(playlist: ExportedPlaylist, context: Context): Playlist? =
    MediaStorePlaylists.searchByPath(context, playlist.path)