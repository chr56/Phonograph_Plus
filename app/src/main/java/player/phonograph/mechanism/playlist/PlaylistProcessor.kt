/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist

import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.mechanism.IFavorite
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.mechanism.playlist.mediastore.addToPlaylistViaMediastore
import player.phonograph.mechanism.playlist.mediastore.moveItemViaMediastore
import player.phonograph.mechanism.playlist.mediastore.removeFromPlaylistViaMediastore
import player.phonograph.mechanism.playlist.mediastore.renamePlaylistViaMediastore
import player.phonograph.mechanism.playlist.saf.appendToPlaylistViaSAF
import player.phonograph.model.Song
import player.phonograph.model.playlist.DatabasePlaylistLocation
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.PLAYLIST_TYPE_FAVORITE
import player.phonograph.model.playlist.PLAYLIST_TYPE_HISTORY
import player.phonograph.model.playlist.PLAYLIST_TYPE_LAST_ADDED
import player.phonograph.model.playlist.PLAYLIST_TYPE_MY_TOP_TRACK
import player.phonograph.model.playlist.PLAYLIST_TYPE_RANDOM
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.VirtualPlaylistLocation
import player.phonograph.repo.database.SongPlayCountStore
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.repo.mediastore.loaders.RecentlyPlayedTracksLoader
import player.phonograph.repo.mediastore.loaders.TopTracksLoader
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.concurrent.coroutineToast
import android.content.Context

object PlaylistProcessors {

    fun reader(playlist: Playlist): PlaylistReader = of(playlist) as PlaylistReader
    fun writer(playlist: Playlist, preferSAF: Boolean = true): PlaylistWriter? =
        of(playlist, preferSAF) as? PlaylistWriter

    private fun of(playlist: Playlist, preferSAF: Boolean = true): PlaylistProcessor =
        when (val location = playlist.location) {
            is FilePlaylistLocation     -> FilePlaylistProcessor.by(location, preferSAF)
            is DatabasePlaylistLocation -> TODO()
            is VirtualPlaylistLocation  -> when (location.type) {
                PLAYLIST_TYPE_FAVORITE     -> FavoriteSongsPlaylistProcessor
                PLAYLIST_TYPE_LAST_ADDED   -> LastAddedPlaylistProcessor
                PLAYLIST_TYPE_HISTORY      -> HistoryPlaylistProcessor
                PLAYLIST_TYPE_MY_TOP_TRACK -> MyTopTracksPlaylistProcessor
                PLAYLIST_TYPE_RANDOM       -> ShuffleAllPlaylistProcessor
                else                       -> throw RuntimeException("Unsupported playlist type: ${location.type}")
            }
        }

}

sealed interface PlaylistProcessor

sealed interface PlaylistReader : PlaylistProcessor {
    suspend fun allSongs(context: Context): List<Song>
    suspend fun containsSong(context: Context, songId: Long): Boolean
    suspend fun refresh(context: Context) {}
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


private sealed class FilePlaylistProcessor(val location: FilePlaylistLocation) : PlaylistReader, PlaylistWriter {

    override suspend fun allSongs(context: Context): List<Song> =
        MediaStorePlaylists.songs(context, location).map { it.song }

    override suspend fun containsSong(context: Context, songId: Long): Boolean =
        MediaStorePlaylists.contains(context, location, songId)

    override suspend fun removeSong(context: Context, song: Song, index: Long): Boolean =
        removeFromPlaylistViaMediastore(context, location.storageVolume, location.mediastoreId, song.id, index) > 0

    override suspend fun moveSong(context: Context, from: Int, to: Int): Boolean {
        return moveItemViaMediastore(context, location.mediastoreId, from, to)
    }

    override suspend fun rename(context: Context, newName: String): Boolean =
        renamePlaylistViaMediastore(context, location.storageVolume, location.mediastoreId, newName)


    class MediaStoreImplementation(location: FilePlaylistLocation) : FilePlaylistProcessor(location) {
        override suspend fun appendSong(context: Context, song: Song) = impl(context, listOf(song))
        override suspend fun appendSongs(context: Context, songs: List<Song>) = impl(context, songs)
        private suspend fun impl(context: Context, songs: List<Song>) {
            addToPlaylistViaMediastore(context, songs, location.storageVolume, location.mediastoreId, true)
        }
    }

    class SafImplementation(location: FilePlaylistLocation) : FilePlaylistProcessor(location) {
        override suspend fun appendSong(context: Context, song: Song) = impl(context, listOf(song))
        override suspend fun appendSongs(context: Context, songs: List<Song>) = impl(context, songs)
        private suspend fun impl(context: Context, songs: List<Song>) {
            coroutineToast(context, R.string.direction_open_file_with_saf)
            appendToPlaylistViaSAF(context, songs, location.mediastoreId, location.path)
        }
    }

    companion object {
        @JvmStatic
        fun by(location: FilePlaylistLocation, useSaf: Boolean) =
            if (useSaf) SafImplementation(location) else MediaStoreImplementation(location)
    }

}

private data object FavoriteSongsPlaylistProcessor : PlaylistReader, PlaylistWriter {

    val favorite: IFavorite by GlobalContext.get().inject()

    override suspend fun allSongs(context: Context): List<Song> =
        favorite.allSongs(context)

    override suspend fun containsSong(context: Context, songId: Long): Boolean {
        val song = Songs.id(context, songId) ?: return false
        return favorite.isFavorite(context, song)
    }

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

    private val songPlayCountStore: SongPlayCountStore
        get() = GlobalContext.get().get()

}

private data object ShuffleAllPlaylistProcessor : PlaylistReader {
    override suspend fun allSongs(context: Context): List<Song> = Songs.all(context)
    override suspend fun containsSong(context: Context, songId: Long): Boolean = true
}

private fun notifyMediaStoreChanged() = GlobalContext.get().get<MediaStoreTracker>().notifyAllListeners()

private const val TAG = "PlaylistProcessors"