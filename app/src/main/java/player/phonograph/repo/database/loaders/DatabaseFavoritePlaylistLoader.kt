/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.database.loaders

import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.database.store.FavoritesStore
import player.phonograph.repo.loader.IFavoritePlaylists
import android.content.Context

class DatabaseFavoritePlaylistLoader : IFavoritePlaylists {

    private val favoritesStore: FavoritesStore = FavoritesStore.get()

    override suspend fun allPlaylists(context: Context): List<Playlist> =
        favoritesStore.getAllPlaylists(context)

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
}