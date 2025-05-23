/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo.loader

import player.phonograph.model.Genre
import player.phonograph.model.Song
import android.content.Context

interface IGenres {

    suspend fun all(context: Context): List<Genre>

    suspend fun id(context: Context, id: Long): Genre?

    suspend fun songs(context: Context, genreId: Long): List<Song>

}