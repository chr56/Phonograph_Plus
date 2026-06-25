/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Artist
import player.phonograph.model.repo.loader.IArtists
import player.phonograph.repo.room.converter.EntityConverter
import android.content.Context

object RoomArtists : RoomLoader(), IArtists {

    override suspend fun all(context: Context): List<Artist> =
        db.ArtistQueryDao().all(artistSortMode(context)).map(EntityConverter::toArtistModel)

    override suspend fun id(context: Context, id: Long): Artist =
        db.ArtistQueryDao().id(id)?.let(EntityConverter::toArtistModel) ?: Artist()

    override suspend fun searchByName(context: Context, query: String): List<Artist> =
        db.ArtistQueryDao().searchByName("%$query%").map(EntityConverter::toArtistModel)

}
