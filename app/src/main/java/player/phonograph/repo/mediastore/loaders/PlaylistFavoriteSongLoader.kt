/*
 *  Copyright (c) 2022~2025 chr_56
 */
package player.phonograph.repo.mediastore.loaders

import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistManager
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.mechanism.playlist.mediastore.addToPlaylistViaMediastore
import player.phonograph.mechanism.playlist.mediastore.createPlaylistViaMediastore
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.IFavoriteSongs
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.util.concurrent.coroutineToast
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


class PlaylistFavoriteSongLoader : IFavoriteSongs {


    override suspend fun allSongs(context: Context): List<Song> {
        val favoritesPlaylist = getFavoritesPlaylist(context)
        return if (favoritesPlaylist != null) {
            PlaylistProcessors.reader(favoritesPlaylist).allSongs(context)
        } else {
            emptyList()
        }
    }

    override suspend fun isFavorite(context: Context, song: Song): Boolean {
        val favoritesPlaylist = getFavoritesPlaylist(context)
        return if (favoritesPlaylist != null) {
            MediaStorePlaylists.contains(context, favoritesPlaylist.location, song.id)
        } else {
            false
        }
    }

    override suspend fun toggleFavorite(context: Context, song: Song): Boolean {
        return runBlocking {
            if (isFavorite(context, song)) {
                val favoritesPlaylist = getFavoritesPlaylist(context)
                if (favoritesPlaylist != null)
                    PlaylistProcessors.writer(favoritesPlaylist)!!.removeSong(context, song, -1)
                false
            } else {
                val favoritesPlaylist = getOrCreateFavoritesPlaylist(context)
                if (favoritesPlaylist != null) {
                    val location = favoritesPlaylist.location as FilePlaylistLocation
                    addToPlaylistViaMediastore(
                        context,
                        song,
                        location.storageVolume,
                        location.mediastoreId,
                        false
                    )
                    true
                } else {
                    coroutineToast(context, R.string.failed)
                    false
                }
            }
        }
    }

    override suspend fun cleanMissed(context: Context): Boolean = false

    override suspend fun clearAll(context: Context): Boolean {
        val favoritesPlaylist = getFavoritesPlaylist(context)
        return if (favoritesPlaylist != null) {
            PlaylistManager.delete(favoritesPlaylist, true).delete(context)
        } else {
            false
        }
    }

    private suspend fun getFavoritesPlaylist(context: Context): Playlist? =
        MediaStorePlaylists.named(context, context.getString(R.string.favorites))

    private suspend fun getOrCreateFavoritesPlaylist(context: Context): Playlist? {
        return createOrFindPlaylistViaMediastore(context, context.getString(R.string.favorites))
    }


    /**
     * find or create playlist via MediaStore
     * @return playlist created or found
     */
    private suspend fun createOrFindPlaylistViaMediastore(
        context: Context,
        name: String,
    ): Playlist? = withContext(Dispatchers.IO) {
        require(name.isNotEmpty())
        // query first
        val playlists = MediaStorePlaylists.searchByName(context, name)
        if (playlists.isNotEmpty()) {
            playlists.first()
        } else {
            val id = createPlaylistViaMediastore(context, name)
            if (id != -1L) {
                MediaStorePlaylists.id(context, id)
            } else {
                null
            }
        }
    }
}