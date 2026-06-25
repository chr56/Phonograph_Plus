/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.PlaylistSongEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class PlaylistSongManipulateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(playlist: PlaylistSongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(playlist: Collection<PlaylistSongEntity>): List<Long>

    @Update
    abstract suspend fun update(playlist: PlaylistSongEntity)

    @Delete
    abstract suspend fun delete(playlist: PlaylistSongEntity): Int

    @Transaction
    open suspend fun removeItem(queryDao: PlaylistSongQueryDao, playlistId: Long, songId: Long, position: Int): Boolean {
        val targetEntity = queryDao.at(playlistId, position) ?: return false
        return if (targetEntity.mediastoreId == songId) {
            delete(targetEntity)
            val max = queryDao.maximumIndexOf(playlistId)
            if (max > position) for (i in position + 1..max) {
                queryDao.at(playlistId, i)?.let { update(it.copy(position = it.position - 1)) }
            }
            true
        } else {
            false
        }
    }

    @Transaction
    open suspend fun move(queryDao: PlaylistSongQueryDao, playlistId: Long, from: Int, to: Int): Boolean {
        if (from == to) return true
        val targetEntity = queryDao.at(playlistId, from) ?: return false
        update(targetEntity.copy(position = -1))

        val range = if (from < to) {
            from + 1..to
        } else {
            to..from - 1
        }
        val delta = if (from < to) -1 else +1
        for (position in range) {
            queryDao.at(playlistId, position)?.let { update(it.copy(position = it.position + delta)) }
        }

        update(targetEntity.copy(position = to))
        return true
    }

    @Transaction
    open suspend fun swap(queryDao: PlaylistSongQueryDao, playlistId: Long, positionA: Int, positionB: Int): Boolean {
        val a = queryDao.at(playlistId, positionA) ?: return false
        val b = queryDao.at(playlistId, positionB) ?: return false
        update(a.copy(position = positionB))
        update(b.copy(position = positionA))
        return true
    }
}
