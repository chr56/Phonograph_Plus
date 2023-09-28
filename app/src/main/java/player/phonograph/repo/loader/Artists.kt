/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Artist
import player.phonograph.repo.mediastore.loaders.ArtistLoader
import android.content.Context

object Artists {

    fun all(context: Context): List<Artist> = ArtistLoader.all(context)

    fun id(context: Context, id: Long): Artist = ArtistLoader.id(context, id)

    fun searchByName(context: Context, query: String) = ArtistLoader.searchByName(context, query)

}