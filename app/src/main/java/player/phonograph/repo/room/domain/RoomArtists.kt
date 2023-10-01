/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Artist
import player.phonograph.model.repo.loader.IArtists
import player.phonograph.repo.room.converter.EntityConverter
import player.phonograph.repo.room.dao.RoomSortOrder.defaultArtistSortMode
import android.content.Context

object RoomArtists : RoomLoader(), IArtists {

    override suspend fun all(context: Context): List<Artist> =
        db.ArtistDao().all(artistSortMode(context)).map(EntityConverter::toArtistModel)

    override suspend fun id(context: Context, id: Long): Artist =
        db.ArtistDao().id(id)?.let(EntityConverter::toArtistModel) ?: Artist()

    override suspend fun searchByName(context: Context, query: String): List<Artist> =
        db.QueryDao().artistsWithName(query, defaultArtistSortMode).map(EntityConverter::toArtistModel)

}