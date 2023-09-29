/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Genre
import player.phonograph.model.Song
import android.content.Context

interface IGenres {

    fun all(context: Context): List<Genre>

    fun id(context: Context, id: Long): Genre?

    fun songs(context: Context, genreId: Long): List<Song>

}