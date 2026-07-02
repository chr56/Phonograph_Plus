/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Album
import player.phonograph.model.repo.loader.IAlbums
import player.phonograph.repo.room.converter.EntityConverter
import player.phonograph.repo.room.dao.RoomSortOrder.defaultArtistSortMode
import android.content.Context

object RoomAlbums : RoomLoader(), IAlbums {

    override suspend fun all(context: Context): List<Album> =
        db.AlbumQueryDao().all(albumSortMode(context)).map(EntityConverter::toAlbumModel)

    override suspend fun id(context: Context, id: Long): Album =
        db.AlbumQueryDao().id(id)?.let(EntityConverter::toAlbumModel) ?: Album()

    override suspend fun searchByName(context: Context, query: String): List<Album> =
        db.AlbumQueryDao().searchByName("%$query%").map(EntityConverter::toAlbumModel)

    override suspend fun artist(context: Context, artistId: Long): List<Album> =
        db.ArtistQueryDao().artistAlbums(artistId)?.albumEntities.orEmpty()
            .sortedByDescending { it.year }.map(EntityConverter::toAlbumModel)

}
