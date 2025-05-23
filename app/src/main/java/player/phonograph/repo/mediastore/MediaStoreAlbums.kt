/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.repo.loader.IAlbums
import player.phonograph.repo.mediastore.loaders.AlbumLoader
import player.phonograph.repo.mediastore.loaders.ArtistAlbumLoader
import android.content.Context

object MediaStoreAlbums : IAlbums {

    override suspend fun all(context: Context) = AlbumLoader.all(context)

    override suspend fun id(context: Context, id: Long) = AlbumLoader.id(context, id)

    override suspend fun searchByName(context: Context, query: String) = AlbumLoader.searchByName(context, query)

    override suspend fun artist(context: Context, artistId: Long) = ArtistAlbumLoader.id(context, artistId)

}