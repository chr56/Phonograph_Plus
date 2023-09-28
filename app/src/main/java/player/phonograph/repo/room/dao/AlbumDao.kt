/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.Tables.ALBUMS
import androidx.room.*

@Dao
interface AlbumDao {

    @Query("SELECT * from $ALBUMS order by :sortOrder")
    fun all(sortOrder: String): List<AlbumEntity>

}
