/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns.MEDIASTORE_ID
import player.phonograph.repo.room.entity.Columns.PATH
import player.phonograph.repo.room.entity.FavoriteSongEntity
import player.phonograph.repo.room.entity.Tables.FAVORITE_SONGS
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class FavoriteSongManipulateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun add(song: FavoriteSongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun add(songs: Collection<FavoriteSongEntity>): LongArray

    @Query("DELETE FROM $FAVORITE_SONGS where $MEDIASTORE_ID = :id and $PATH = :path")
    abstract suspend fun remove(id: Long, path: String): Int

    @Query("DELETE FROM $FAVORITE_SONGS")
    abstract suspend fun purge(): Int
}
