/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.PlaylistEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

@Dao
abstract class PlaylistManipulateDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    abstract suspend fun update(playlist: PlaylistEntity)

    @Delete
    abstract suspend fun delete(playlist: PlaylistEntity): Int

    suspend fun rename(queryDao: PlaylistQueryDao, id: Long, newName: String): Boolean =
        update(id, queryDao) { it.copy(name = newName) }

    suspend fun modifyDate(queryDao: PlaylistQueryDao, id: Long, timestamp: Long): Boolean =
        update(id, queryDao) { it.copy(dateModified = timestamp) }

    private suspend fun update(id: Long, queryDao: PlaylistQueryDao, action: (PlaylistEntity) -> PlaylistEntity): Boolean {
        val entity = queryDao.id(id) ?: return false
        update(action(entity))
        return true
    }
}
