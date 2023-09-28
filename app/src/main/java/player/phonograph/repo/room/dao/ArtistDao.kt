/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Tables.ARTISTS
import androidx.room.Dao
import androidx.room.Query

@Dao
interface ArtistDao {

    @Query("SELECT * from $ARTISTS order by :sortOrder")
    fun all(sortOrder: String = ARTIST_ID): List<ArtistEntity>

}