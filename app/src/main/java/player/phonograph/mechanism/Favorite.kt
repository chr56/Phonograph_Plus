/*
 *  Copyright (c) 2022~2023 chr_56
 */
package player.phonograph.mechanism

import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.repo.mediastore.loaders.PlaylistSongLoader
import player.phonograph.mechanism.playlist.mediastore.addToPlaylistViaMediastore
import player.phonograph.mechanism.playlist.mediastore.createOrFindPlaylistViaMediastore
import player.phonograph.mechanism.playlist.mediastore.removeFromPlaylistViaMediastore
import android.content.Context
import kotlinx.coroutines.runBlocking


interface IFavorite {

    suspend fun allSongs(context: Context): List<Song>

    suspend fun isFavorite(context: Context, song: Song): Boolean

    /**
     * @return new favorite state
     */
    fun toggleFavorite(context: Context, song: Song): Boolean

    fun clearAll(context: Context)

}

class FavoriteDatabaseImpl : IFavorite {

    private val favoritesStore by GlobalContext.get().inject<FavoritesStore>()

    override suspend fun allSongs(context: Context): List<Song> =
        favoritesStore.getAllSongs(context)

    override suspend fun isFavorite(context: Context, song: Song): Boolean =
        favoritesStore.containsSong(song.id, song.data)

    override fun toggleFavorite(context: Context, song: Song): Boolean = runBlocking {
        if (isFavorite(context, song)) {
            !favoritesStore.removeSong(song)
        } else {
            favoritesStore.addSong(song)
        }
    }

    override fun clearAll(context: Context) {
        favoritesStore.clearAll()
    }
}

class FavoritePlaylistImpl : IFavorite {


    override suspend fun allSongs(context: Context): List<Song> = runBlocking {
        val favoritesPlaylist = getFavoritesPlaylist(context)
        favoritesPlaylist?.getSongs(context) ?: emptyList()
    }

    override suspend fun isFavorite(context: Context, song: Song): Boolean {
        val favoritesPlaylist = getFavoritesPlaylist(context)
        return if (favoritesPlaylist != null) {
            PlaylistSongLoader.doesPlaylistContain(context, favoritesPlaylist.id, song.id)
        } else {
            false
        }
    }

    override fun toggleFavorite(context: Context, song: Song): Boolean {
        return runBlocking {
            if (isFavorite(context, song)) {
                val favoritesPlaylist = getFavoritesPlaylist(context)
                if (favoritesPlaylist != null)
                    removeFromPlaylistViaMediastore(context, favoritesPlaylist.id, song.id)
                false
            } else {
                addToPlaylistViaMediastore(context, song, getOrCreateFavoritesPlaylist(context).id, false)
                true
            }
        }
    }

    override fun clearAll(context: Context) {
        getFavoritesPlaylist(context)?.clear(context)
    }

    private fun getFavoritesPlaylist(context: Context): FilePlaylist? {
        return PlaylistLoader.playlistName(context, context.getString(R.string.favorites)).takeIf { it.id > 0 }
    }

    private suspend fun getOrCreateFavoritesPlaylist(context: Context): Playlist {
        return PlaylistLoader.id(
            context,
            createOrFindPlaylistViaMediastore(context, context.getString(R.string.favorites))
        )
    }
}