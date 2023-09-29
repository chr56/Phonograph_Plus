/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.loader

import player.phonograph.model.Artist
import player.phonograph.repo.loader.IArtists
import player.phonograph.repo.room.artistSortMode
import player.phonograph.repo.room.defaultArtistSortMode
import android.content.Context

object RoomArtists : BaseLoader(), IArtists {

    override fun all(context: Context): List<Artist> =
        db.ArtistDao().all(artistSortMode(context)).convertArtists()

    override fun id(context: Context, id: Long): Artist = db.ArtistDao().id(id).convert()

    override fun searchByName(context: Context, query: String): List<Artist> =
        db.QueryDao().artistsWithName(query, defaultArtistSortMode).convertArtists()

}