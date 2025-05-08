/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.backup

import lib.storage.textparser.ExternalFilePathParser
import okio.BufferedSink
import okio.Source
import okio.buffer
import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.Song
import player.phonograph.model.backup.ExportedFavorites
import player.phonograph.model.backup.ExportedPathFilter
import player.phonograph.model.backup.ExportedPlayingQueue
import player.phonograph.model.backup.ExportedPlaylist
import player.phonograph.model.backup.ExportedSong
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.database.store.FavoritesStore
import player.phonograph.repo.database.store.PathFilterStore
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.service.queue.MusicPlaybackQueueStore
import player.phonograph.service.queue.QueueManager
import player.phonograph.util.reportError
import player.phonograph.util.warning
import android.content.Context
import kotlin.LazyThreadSafetyMode.NONE
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

object DatabaseDataManger {

    @Suppress("unused")
    fun exportPathFilter(sink: BufferedSink, context: Context): Boolean {

        val db = PathFilterStore.get()
        val whitelist = db.whitelistPaths
        val blacklist = db.blacklistPaths

        val exported = parser.encodeToString(
            ExportedPathFilter(ExportedPathFilter.VERSION, whitelist, blacklist)
        )
        return write(sink, "PathFilter", exported)
    }

    @Suppress("unused")
    fun importPathFilter(context: Context, source: Source): Boolean {
        val imported = read<ExportedPathFilter>(source, "PathFilter")

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

    @Suppress("unused")
    fun exportPlayingQueues(sink: BufferedSink, context: Context): Boolean {
        val db = GlobalContext.get().get<MusicPlaybackQueueStore>()
        val originalPlayingQueue = db.savedOriginalPlayingQueue.map(::exportSong)
        val playingQueue = db.savedPlayingQueue.map(::exportSong)

        val exported = parser.encodeToString(
            ExportedPlayingQueue(ExportedPlayingQueue.VERSION, playingQueue, originalPlayingQueue)
        )
        return write(sink, "PlayingQueues", exported)
    }

    fun importPlayingQueues(context: Context, source: Source): Boolean {
        val imported = read<ExportedPlayingQueue>(source, "PlayingQueues")

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

    suspend fun exportFavorites(sink: BufferedSink, context: Context): Boolean {

        val db = FavoritesStore.get()

        val songs =
            db.getAllSongs { _, path, _, _ -> lookupSong(context, path) }.map(::exportSong)

        val playlists =
            db.getAllPlaylists { id, path, _, _ -> lookupPlaylist(context, id, path) }.map(::exportPlaylist)


        val exported = parser.encodeToString(
            ExportedFavorites(ExportedFavorites.VERSION, songs, playlists)
        )
        return write(sink, "Favorites", exported)
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

    fun importFavorites(context: Context, source: Source): Boolean {
        val imported = read<ExportedFavorites>(source, "Favorites")

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

    private fun exportSong(song: Song): ExportedSong =
        ExportedSong(song.data, song.title, song.albumName, song.artistName)

    private fun importSong(song: ExportedSong, context: Context): Song? =
        runBlocking { Songs.searchByPath(context, song.path, withoutPathFilter = true).firstOrNull() }

    private fun exportPlaylist(playlist: Playlist): ExportedPlaylist =
        ExportedPlaylist(playlist.path() ?: "-", playlist.name)

    private fun importPlaylist(playlist: ExportedPlaylist, context: Context): Playlist? =
        MediaStorePlaylists.searchByPath(context, playlist.path)

    private fun write(sink: BufferedSink, name: String, serialized: String): Boolean = try {
        if (serialized.isNotEmpty()) {
            sink.writeString(serialized, Charsets.UTF_8)
            true
        } else {
            warning(TAG, "$name: Nothing to export")
            false
        }
    } catch (e: IOException) {
        reportError(e, TAG, "Failed to export $name!")
        false
    }

    private inline fun <reified T> read(source: Source, name: String): T? {
        val rawString = source.buffer().use { bufferedSource -> bufferedSource.readUtf8() }
        val imported =
            try {
                parser.decodeFromString<T>(rawString)
            } catch (e: SerializationException) {
                reportError(e, TAG, "Failed to read $name Backup!")
                null
            }
        return imported
    }

    private val parser by lazy(NONE) {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    private const val TAG = "DatabaseBackupManger"
}