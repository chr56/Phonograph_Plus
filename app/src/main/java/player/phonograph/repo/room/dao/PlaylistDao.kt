/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.PlaylistEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    @Transaction
    protected open fun update(id: Long, action: (PlaylistEntity) -> PlaylistEntity): Boolean {
        val entity = id(id)
        return if (entity != null) {
            update(action(entity))
            true
        } else {
            false
        }
    }

    fun rename(id: Long, newName: String): Boolean =
        update(id) { entity ->
            entity.copy(name = newName)
        }

    fun modifyDate(id: Long, timestamp: Long): Boolean =
        update(id) { entity ->
            entity.copy(dateModified = timestamp)
        }

}