/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.database.loaders

import okhttp3.internal.toLongOrDefault
import player.phonograph.model.playlist.DatabasePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.repo.loader.IFavoritePlaylists
import player.phonograph.repo.database.store.FavoritesStore
import player.phonograph.repo.loader.Playlists
import player.phonograph.repo.mediastore.MediaStorePlaylists
import android.content.Context

class DatabaseFavoritePlaylistLoader : IFavoritePlaylists {

    private val favoritesStore: FavoritesStore = FavoritesStore.get()

    override suspend fun allPlaylists(context: Context): List<Playlist> =
        favoritesStore.getAllPlaylists { id, path, _, _ -> lookupPlaylist(context, id, path) }

    override suspend fun isFavorite(context: Context, id: Long?, path: String?): Boolean =
        favoritesStore.containsPlaylist(id, path)

    override suspend fun isFavorite(context: Context, playlist: Playlist): Boolean =
        favoritesStore.containsPlaylist(playlist)

    override suspend fun addToFavorites(context: Context, playlist: Playlist): Boolean =
        favoritesStore.addPlaylist(playlist)

    override suspend fun removeFromFavorites(context: Context, playlist: Playlist): Boolean =
        favoritesStore.removePlaylist(playlist)

    override suspend fun toggleFavorite(context: Context, playlist: Playlist): Boolean =
        if (isFavorite(context, playlist)) {
            !removeFromFavorites(context, playlist)
        } else {
            addToFavorites(context, playlist)
        }

    override suspend fun clearAll(context: Context): Boolean {
        favoritesStore.clearAllPlaylists()
        return true
    }

    companion object {
        private suspend fun lookupPlaylist(context: Context, id: Long, path: String): Playlist? {

            val filePlaylist = MediaStorePlaylists.searchByPath(context, path)
            if (filePlaylist != null) return filePlaylist

            val databasePlaylist = Playlists.of(context, DatabasePlaylistLocation(path.toLongOrDefault(0)))
            if (databasePlaylist != null) return databasePlaylist

            return null
        }
    }
}