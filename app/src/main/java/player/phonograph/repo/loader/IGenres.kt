/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Genre
import player.phonograph.model.Song
import android.content.Context

interface IGenres {

    suspend fun all(context: Context): List<Genre>

    suspend fun id(context: Context, id: Long): Genre?

    suspend fun songs(context: Context, genreId: Long): List<Song>

}