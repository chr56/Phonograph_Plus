/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.playlist.Playlist
import android.content.Context

interface IFavoritePlaylists {

    suspend fun allPlaylists(context: Context): List<Playlist>

    suspend fun isFavorite(context: Context, playlist: Playlist): Boolean

    suspend fun addToFavorites(context: Context, playlist: Playlist): Boolean

    suspend fun removeFromFavorites(context: Context, playlist: Playlist): Boolean

    /**
     * @return new favorite state
     */
    suspend fun toggleFavorite(context: Context, playlist: Playlist): Boolean

    /**
     * clear all
     */
    suspend fun clearAll(context: Context): Boolean

}