/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.R
import player.phonograph.foundation.mediastore.mediastoreUriPlaylist
import player.phonograph.mechanism.playlist.PlaylistActions
import player.phonograph.mechanism.playlist.PlaylistSongsActions
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.repo.loader.IFavoriteSongs
import player.phonograph.util.concurrent.coroutineToast
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistFavoriteSongs : IFavoriteSongs {

    override suspend fun all(context: Context): List<Song> {
        val favoritesPlaylist = getFavoritesPlaylist(context)
        return if (favoritesPlaylist != null) {
            PlaylistSongsActions.reader(favoritesPlaylist).allSongs(context)
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

    override suspend fun add(context: Context, song: Song): Boolean = add(context, listOf(song))

    override suspend fun add(context: Context, songs: List<Song>): Boolean {
        val favoritesPlaylist = getOrCreateFavoritesPlaylist(context)
        return if (favoritesPlaylist != null) {
            val location = favoritesPlaylist.location as FilePlaylistLocation
            val playlistUri = mediastoreUriPlaylist(location.storageVolume, location.mediastoreId)
            return MediaStorePlaylistsActions.amendSongs(context, playlistUri, songs)
        } else {
            coroutineToast(context, context.getString(R.string.err_could_not_create_playlist))
            false
        }
    }

    override suspend fun remove(context: Context, song: Song): Boolean {
        val favoritesPlaylist = getFavoritesPlaylist(context)
        return if (favoritesPlaylist != null) {
            !PlaylistSongsActions.writer(favoritesPlaylist)!!.removeSong(context, song, -1)
        } else {
            true
        }
    }

    override suspend fun toggleState(context: Context, song: Song): Boolean =
        if (isFavorite(context, song)) remove(context, song) else add(context, song)

    override suspend fun cleanMissing(context: Context): Boolean = false

    override suspend fun clearAll(context: Context): Boolean {
        val favoritesPlaylist = getFavoritesPlaylist(context)
        return if (favoritesPlaylist != null) {
            PlaylistActions.delete(favoritesPlaylist, true).delete(context)
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