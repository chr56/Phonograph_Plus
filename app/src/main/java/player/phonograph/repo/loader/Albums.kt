/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Album
import player.phonograph.repo.mediastore.loaders.AlbumLoader
import player.phonograph.repo.mediastore.loaders.ArtistAlbumLoader
import android.content.Context

object Albums {

    fun all(context: Context): List<Album> = AlbumLoader.all(context)

    fun id(context: Context, id: Long): Album = AlbumLoader.id(context, id)

    fun searchByName(context: Context, query: String) = AlbumLoader.searchByName(context, query)

    fun artist(context: Context, artistId: Long): List<Album> = ArtistAlbumLoader.id(context, artistId)

}