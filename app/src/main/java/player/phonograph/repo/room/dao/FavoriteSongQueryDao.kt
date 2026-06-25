/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns.MEDIASTORE_ID
import player.phonograph.repo.room.entity.Columns.PATH
import player.phonograph.repo.room.entity.FavoriteSongEntity
import player.phonograph.repo.room.entity.Tables.FAVORITE_SONGS
import androidx.room.Dao
import androidx.room.Query

@Dao
abstract class FavoriteSongQueryDao {

    @Query("SELECT * from $FAVORITE_SONGS order by DATE_ADDED DESC")
    abstract suspend fun all(): List<FavoriteSongEntity>

    @Query("SELECT EXISTS(SELECT 1 from $FAVORITE_SONGS where $MEDIASTORE_ID = :id or $PATH = :path)")
    abstract suspend fun contains(id: Long, path: String): Boolean
}
