/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist

import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.mechanism.IFavorite
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.mechanism.playlist.mediastore.addToPlaylistViaMediastore
import player.phonograph.mechanism.playlist.mediastore.createPlaylistViaMediastore
import player.phonograph.mechanism.playlist.mediastore.deletePlaylistsViaMediastore
import player.phonograph.mechanism.playlist.mediastore.duplicatePlaylistViaMediaStore
import player.phonograph.mechanism.playlist.mediastore.moveItemViaMediastore
import player.phonograph.mechanism.playlist.mediastore.removeFromPlaylistViaMediastore
import player.phonograph.mechanism.playlist.mediastore.renamePlaylistViaMediastore
import player.phonograph.mechanism.playlist.saf.appendToPlaylistViaSAF
import player.phonograph.mechanism.playlist.saf.createPlaylistViaSAF
import player.phonograph.mechanism.playlist.saf.createPlaylistsViaSAF
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.PLAYLIST_TYPE_FAVORITE
import player.phonograph.model.playlist.PLAYLIST_TYPE_HISTORY
import player.phonograph.model.playlist.PLAYLIST_TYPE_LAST_ADDED
import player.phonograph.model.playlist.PLAYLIST_TYPE_MY_TOP_TRACK
import player.phonograph.model.playlist.PLAYLIST_TYPE_RANDOM
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.VirtualPlaylistLocation
import player.phonograph.repo.database.HistoryStore
import player.phonograph.repo.database.SongPlayCountStore
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.loaders.PlaylistSongLoader
import player.phonograph.repo.mediastore.loaders.RecentlyPlayedTracksLoader
import player.phonograph.repo.mediastore.loaders.TopTracksLoader
import player.phonograph.settings.Keys
import player.phonograph.settings.PLAYLIST_OPS_BEHAVIOUR_AUTO
import player.phonograph.settings.PLAYLIST_OPS_BEHAVIOUR_FORCE_LEGACY
import player.phonograph.settings.PLAYLIST_OPS_BEHAVIOUR_FORCE_SAF
import player.phonograph.settings.Setting
import player.phonograph.util.coroutineToast
import player.phonograph.util.file.selectDocumentUris
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffix
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

object PlaylistProcessors {

    fun reader(playlist: Playlist): PlaylistReader = of(playlist) as PlaylistReader
    fun writer(playlist: Playlist): PlaylistWriter? = of(playlist) as? PlaylistWriter

    private fun of(playlist: Playlist): PlaylistProcessor =
        when (val location = playlist.location) {
            is FilePlaylistLocation    -> FilePlaylistProcessor(playlist.id, location.path)
            is VirtualPlaylistLocation -> when (location.type) {
                PLAYLIST_TYPE_FAVORITE     -> FavoriteSongsPlaylistProcessor
                PLAYLIST_TYPE_LAST_ADDED   -> LastAddedPlaylistProcessor
                PLAYLIST_TYPE_HISTORY      -> HistoryPlaylistProcessor
                PLAYLIST_TYPE_MY_TOP_TRACK -> MyTopTracksPlaylistProcessor
                PLAYLIST_TYPE_RANDOM       -> ShuffleAllPlaylistProcessor
                else                       -> throw RuntimeException("Unsupported playlist type: ${location.type}")
            }
        }


    suspend fun create(context: Context, name: String, songs: List<Song>) =
        if (shouldUseSAF(context) && context is ICreateFileStorageAccessible) {
            createPlaylistViaSAF(context, playlistName = name, songs = songs)
        } else {
            createPlaylistViaMediastore(context, name, songs)
        }

    suspend fun duplicate(context: Context, playlist: Playlist) =
        create(context, playlist.name + dateTimeSuffix(currentDate()), reader(playlist).allSongs(context))

    suspend fun duplicate(context: Context, playlists: List<Playlist>) {
        val names = playlists.map { it.name }
        val songBatches = playlists.map { reader(it).allSongs(context) }
        if (shouldUseSAF(context) && context is ICreateFileStorageAccessible) {
            createPlaylistsViaSAF(context, songBatches, names, defaultDirectory.absolutePath)
        } else {
            // todo
            duplicatePlaylistViaMediaStore(context, songBatches, names)
        }
    }

    suspend fun delete(context: Context, playlist: Playlist, options: Any? = null): Boolean =
        deleteImpl(context, playlist, options)

    private val defaultDirectory: File get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

    const val OPTION_DELETE_WITH_SAF = "SAF"
    const val OPTION_DELETE_WITH_MEDIASTORE = "MEDIASTORE"
}

sealed interface PlaylistProcessor

sealed interface PlaylistReader : PlaylistProcessor {
    suspend fun allSongs(context: Context): List<Song>
    suspend fun containsSong(context: Context, songId: Long): Boolean
    suspend fun refresh(context: Context) {}
    suspend fun clear(context: Context, options: Any? = null): Boolean = false
}

sealed interface PlaylistWriter : PlaylistProcessor {
    suspend fun removeSong(context: Context, song: Song, index: Long): Boolean
    suspend fun removeSongs(context: Context, songs: List<Song>) {
        for (song in songs) {
            removeSong(context, song, -1)
        }
    }

    //    fun insert(context: Context, song: Song, pos: Int)
    suspend fun appendSong(context: Context, song: Song)
    suspend fun appendSongs(context: Context, songs: List<Song>) {
        for (song in songs) {
            appendSong(context, song)
        }
    }

    suspend fun moveSong(context: Context, from: Int, to: Int): Boolean
    suspend fun rename(context: Context, newName: String): Boolean = false
}


private class FilePlaylistProcessor(val id: Long, val path: String) : PlaylistReader, PlaylistWriter {

    override suspend fun allSongs(context: Context): List<Song> =
        PlaylistSongLoader.getPlaylistSongList(context, id).map { it.song }

    override suspend fun containsSong(context: Context, songId: Long): Boolean =
        PlaylistSongLoader.doesPlaylistContain(context, id, songId)

    override suspend fun removeSong(context: Context, song: Song, index: Long): Boolean {
        return removeFromPlaylistViaMediastore(context, id, song.id, index) > 0
    }

    override suspend fun moveSong(context: Context, from: Int, to: Int): Boolean {
        return moveItemViaMediastore(context, id, from, to)
    }

    override suspend fun appendSong(context: Context, song: Song) {
        if (shouldUseSAF(context) && context is IOpenFileStorageAccessible) {
            coroutineToast(context, R.string.direction_open_file_with_saf)
            appendToPlaylistViaSAF(context, listOf(song), id, path)
        } else {
            addToPlaylistViaMediastore(context, listOf(song), id, true)
        }
    }

    override suspend fun appendSongs(context: Context, songs: List<Song>) {
        if (shouldUseSAF(context) && context is IOpenFileStorageAccessible) {
            coroutineToast(context, R.string.direction_open_file_with_saf)
            appendToPlaylistViaSAF(context, songs, id, path)
        } else {
            addToPlaylistViaMediastore(context, songs, id, true)
        }
    }

    override suspend fun rename(context: Context, newName: String): Boolean =
        renamePlaylistViaMediastore(context, id, newName)


    override suspend fun clear(context: Context, options: Any?): Boolean {
        if (options == PlaylistProcessors.OPTION_DELETE_WITH_MEDIASTORE) {
            val results = deletePlaylistsViaMediastore(context, longArrayOf(id)).firstOrNull() ?: return false
            return results > 0
        } else {
            val uri = selectDocumentUris(context, listOf(path)).firstOrNull() ?: return false
            return DocumentsContract.deleteDocument(context.contentResolver, uri)
        }
    }
}

private data object FavoriteSongsPlaylistProcessor : PlaylistReader, PlaylistWriter {

    val favorite: IFavorite by GlobalContext.get().inject()

    override suspend fun allSongs(context: Context): List<Song> =
        favorite.allSongs(context)

    override suspend fun containsSong(context: Context, songId: Long): Boolean =
        favorite.isFavorite(context, Songs.id(context, songId))

    override suspend fun clear(context: Context, options: Any?) = favorite.clearAll(context)

    override suspend fun removeSong(context: Context, song: Song, index: Long): Boolean =
        favorite.toggleFavorite(context, song)

    override suspend fun appendSong(context: Context, song: Song) {
        favorite.toggleFavorite(context, song)
        notifyMediaStoreChanged()
    }

    override suspend fun appendSongs(context: Context, songs: List<Song>) {
        for (song in songs) favorite.toggleFavorite(context, song)
        notifyMediaStoreChanged()
    }

    override suspend fun moveSong(context: Context, from: Int, to: Int): Boolean = false
}

private data object HistoryPlaylistProcessor : PlaylistReader {
    override suspend fun allSongs(context: Context): List<Song> =
        RecentlyPlayedTracksLoader.get().tracks(context)

    override suspend fun containsSong(context: Context, songId: Long): Boolean = false //todo

    override suspend fun clear(context: Context, options: Any?): Boolean {
        return HistoryStore.get().clear()
    }
}

private data object LastAddedPlaylistProcessor : PlaylistReader {

    override suspend fun allSongs(context: Context): List<Song> =
        Songs.since(context, Setting(context).Composites[Keys.lastAddedCutoffTimeStamp].data / 1000)

    override suspend fun containsSong(context: Context, songId: Long): Boolean =
        allSongs(context).find { it.id == songId } != null

}

private data object MyTopTracksPlaylistProcessor : PlaylistReader {


    override suspend fun allSongs(context: Context): List<Song> =
        TopTracksLoader.get().tracks(context)

    override suspend fun containsSong(context: Context, songId: Long): Boolean = false // todo

    override suspend fun refresh(context: Context) {
        songPlayCountStore.reCalculateScore(context)
    }

    override suspend fun clear(context: Context, options: Any?): Boolean {
        return songPlayCountStore.clear()
    }

    private val songPlayCountStore: SongPlayCountStore
        get() = GlobalContext.get().get()

}

private data object ShuffleAllPlaylistProcessor : PlaylistReader {
    override suspend fun allSongs(context: Context): List<Song> = Songs.all(context)
    override suspend fun containsSong(context: Context, songId: Long): Boolean = true
}

private suspend fun deleteImpl(context: Context, playlist: Playlist, options: Any?): Boolean {
    return when (val location = playlist.location) {
        is FilePlaylistLocation    -> {
            if (options == PlaylistProcessors.OPTION_DELETE_WITH_MEDIASTORE) {
                val results =
                    deletePlaylistsViaMediastore(context, longArrayOf(playlist.id)).firstOrNull() ?: return false
                return results > 0
            } else {
                val uri = selectDocumentUris(context, listOf(location.path)).firstOrNull() ?: return false
                return DocumentsContract.deleteDocument(context.contentResolver, uri)
            }
        }

        is VirtualPlaylistLocation -> when (location.type) {
            PLAYLIST_TYPE_HISTORY      -> HistoryStore.get().clear()
            PLAYLIST_TYPE_FAVORITE     -> GlobalContext.get().get<IFavorite>().clearAll(context)
            PLAYLIST_TYPE_MY_TOP_TRACK -> GlobalContext.get().get<SongPlayCountStore>().clear()
            else                       -> false
        }
    }
}

private fun shouldUseSAF(context: Context): Boolean {
    val preference = Setting(context)[Keys.playlistFilesOperationBehaviour]
    return when (preference.data) {
        PLAYLIST_OPS_BEHAVIOUR_FORCE_SAF    -> true
        PLAYLIST_OPS_BEHAVIOUR_FORCE_LEGACY -> false
        PLAYLIST_OPS_BEHAVIOUR_AUTO         -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        else                                -> {
            preference.data = PLAYLIST_OPS_BEHAVIOUR_AUTO // reset to default
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        }
    }
}

private fun notifyMediaStoreChanged() = GlobalContext.get().get<MediaStoreTracker>().notifyAllListeners()

private const val TAG = "PlaylistProcessors"