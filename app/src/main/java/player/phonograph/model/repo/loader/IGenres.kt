/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.loader

import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import android.content.Context

interface IGenres : Endpoint {

    suspend fun all(context: Context): List<Genre>

    suspend fun all(context: Context, sortMode: SortMode): List<Genre>

    suspend fun id(context: Context, id: Long): Genre?

    suspend fun of(context: Context, songId: Long): List<Genre>

    suspend fun songs(context: Context, genreId: Long): List<Song>

    suspend fun searchByName(context: Context, query: String): List<Genre>

}