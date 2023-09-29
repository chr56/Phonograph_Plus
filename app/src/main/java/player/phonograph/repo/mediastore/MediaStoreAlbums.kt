/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.repo.loader.IAlbums
import player.phonograph.repo.mediastore.loaders.AlbumLoader
import player.phonograph.repo.mediastore.loaders.ArtistAlbumLoader
import android.content.Context

object MediaStoreAlbums : IAlbums {

    override fun all(context: Context) = AlbumLoader.all(context)

    override fun id(context: Context, id: Long) = AlbumLoader.id(context, id)

    override fun searchByName(context: Context, query: String) = AlbumLoader.searchByName(context, query)

    override fun artist(context: Context, artistId: Long) = ArtistAlbumLoader.id(context, artistId)

}