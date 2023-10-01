/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.loader

import player.phonograph.model.Album
import player.phonograph.model.repo.loader.IAlbums
import player.phonograph.repo.room.albumSortMode
import player.phonograph.repo.room.defaultAlbumSortMode
import player.phonograph.repo.room.defaultArtistSortMode
import android.content.Context

object RoomAlbums : BaseLoader(), IAlbums {

    override suspend fun all(context: Context): List<Album> =
        db.AlbumDao().all(albumSortMode(context)).convertAlbums()

    override suspend fun id(context: Context, id: Long): Album =
        db.AlbumDao().id(id).convert()

    override suspend fun searchByName(context: Context, query: String): List<Album> =
        db.QueryDao().albumsWithName(query, defaultAlbumSortMode).convertAlbums()

    override suspend fun artist(context: Context, artistId: Long): List<Album> =
        db.QueryDao().artistAlbums(artistId, defaultArtistSortMode).albumEntities.convertAlbums()

}