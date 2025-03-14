/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Song
import android.content.Context

interface IFavoriteSongs {

    suspend fun allSongs(context: Context): List<Song>

    suspend fun isFavorite(context: Context, song: Song): Boolean

    /**
     * @return new favorite state
     */
    suspend fun toggleFavorite(context: Context, song: Song): Boolean

    /**
     * clean missed (deleted) songs
     */
    suspend fun cleanMissed(context: Context): Boolean

    /**
     * clear all
     */
    suspend fun clearAll(context: Context): Boolean

}