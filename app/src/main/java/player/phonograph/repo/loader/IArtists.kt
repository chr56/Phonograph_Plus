/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Artist
import android.content.Context

interface IArtists {

    fun all(context: Context): List<Artist>

    fun id(context: Context, id: Long): Artist

    fun searchByName(context: Context, query: String): List<Artist>

}