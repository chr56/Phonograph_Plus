/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.loader

import player.phonograph.model.Song
import android.content.Context

interface IFavoriteSongs : Endpoint {

    suspend fun all(context: Context): List<Song>

    suspend fun isFavorite(context: Context, song: Song): Boolean

    suspend fun add(context: Context, song: Song): Boolean

    suspend fun add(context: Context, songs: List<Song>): Boolean

    suspend fun remove(context: Context, song: Song): Boolean

    /**
     * @return new favorite state
     */
    suspend fun toggleState(context: Context, song: Song): Boolean

    /**
     * clean missing (deleted) songs
     */
    suspend fun cleanMissing(context: Context): Boolean

    suspend fun clearAll(context: Context): Boolean

}