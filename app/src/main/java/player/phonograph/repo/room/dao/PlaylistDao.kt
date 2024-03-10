/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.PlaylistEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
abstract class PlaylistDao {

    @Query("SELECT * from ${PlaylistEntity.TABLE_NAME}")
    abstract fun all(): List<PlaylistEntity>

    @Query("SELECT * from ${PlaylistEntity.TABLE_NAME} WHERE ${PlaylistEntity.Columns.ID} =:id")
    abstract fun id(id: Long): PlaylistEntity?

    @Insert
    abstract fun insert(playlist: PlaylistEntity): Long

    @Update
    abstract fun update(playlist: PlaylistEntity)

    @Delete
    abstract fun delete(playlist: PlaylistEntity): Int

}