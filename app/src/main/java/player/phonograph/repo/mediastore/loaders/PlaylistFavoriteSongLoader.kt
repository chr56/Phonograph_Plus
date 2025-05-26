/*
 *  Copyright (c) 2022~2025 chr_56
 */
package player.phonograph.repo.mediastore.loaders

import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistManager
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.repo.loader.IFavoriteSongs
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.repo.mediastore.MediaStorePlaylistsActions
import player.phonograph.util.concurrent.coroutineToast
import player.phonograph.util.mediastoreUriPlaylist
import android.content.Context
import kotlinx.coroutines.Dispatchers
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

    override suspend fun addToFavorites(context: Context, song: Song): Boolean {
        val favoritesPlaylist = getOrCreateFavoritesPlaylist(context)
        return if (favoritesPlaylist != null) {
            val location = favoritesPlaylist.location as FilePlaylistLocation
            val playlistUri = mediastoreUriPlaylist(location.storageVolume, location.mediastoreId)
            return MediaStorePlaylistsActions.amendSongs(context, playlistUri, listOf(song))
        } else {
            coroutineToast(context, context.getString(R.string.err_could_not_create_playlist))
            false
        }
    }

    override suspend fun removeFromFavorites(context: Context, song: Song): Boolean {
        val favoritesPlaylist = getFavoritesPlaylist(context)
        return if (favoritesPlaylist != null) {
            !PlaylistProcessors.writer(favoritesPlaylist)!!.removeSong(context, song, -1)
        } else {
            true
        }
    }

    override suspend fun toggleFavorite(context: Context, song: Song): Boolean =
        if (isFavorite(context, song)) removeFromFavorites(context, song) else addToFavorites(context, song)

    override suspend fun cleanMissing(context: Context): Boolean = false

    override suspend fun clearAll(context: Context): Boolean {
        val favoritesPlaylist = getFavoritesPlaylist(context)
        return if (favoritesPlaylist != null) {
            PlaylistManager.delete(favoritesPlaylist, true).delete(context)
        } else {
            false
        }
    }

    private suspend fun getFavoritesPlaylist(context: Context): Playlist? =
        MediaStorePlaylists.named(context, context.getString(R.string.playlist_favorites))

    private suspend fun getOrCreateFavoritesPlaylist(context: Context): Playlist? {
        return createOrFindPlaylistViaMediastore(context, context.getString(R.string.playlist_favorites))
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
            val uri = MediaStorePlaylistsActions.create(context, name)
            if (uri != null) {
                MediaStorePlaylists.id(context, MediaStorePlaylistsActions.playlistId(uri))
            } else {
                null
            }
        }
    }
}