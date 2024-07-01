/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.PlaylistEntity
import player.phonograph.repo.room.entity.PlaylistSongEntity
import player.phonograph.repo.room.entity.PlaylistWithSongsEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class PlaylistSongDao {

    @Transaction
    @Query("SELECT * FROM ${PlaylistEntity.TABLE_NAME} WHERE ${PlaylistEntity.Columns.ID} =:id")
    abstract fun playlist(id: Long): PlaylistWithSongsEntity?

    @Query(
        "SELECT COALESCE(COUNT(${PlaylistSongEntity.Columns.ID}), 0) " +
                "FROM ${PlaylistSongEntity.TABLE_NAME} " +
                "WHERE ${PlaylistSongEntity.Columns.PLAYLIST_ID} = :playlistId"
    )
    abstract fun size(playlistId: Long): Int

    @Query(
        "SELECT COALESCE(MAX(`${PlaylistSongEntity.Columns.INDEX}`), -1) " +
                "FROM ${PlaylistSongEntity.TABLE_NAME} " +
                "WHERE ${PlaylistSongEntity.Columns.PLAYLIST_ID} = :playlistId"
    )
    abstract fun maximumIndexOf(playlistId: Long): Int


    @Query(
        "SELECT COALESCE(COUNT(${PlaylistSongEntity.Columns.ID}), 0) " +
                "FROM ${PlaylistSongEntity.TABLE_NAME} " +
                "WHERE ${PlaylistSongEntity.Columns.PLAYLIST_ID} = :playlistId " +
                "AND ${PlaylistSongEntity.Columns.MEDIASTORE_ID} = :songId "
    )
    abstract fun count(playlistId: Long, songId: Long): Int


    @Query(
        "SELECT * FROM ${PlaylistSongEntity.TABLE_NAME} " +
                "WHERE ${PlaylistSongEntity.Columns.PLAYLIST_ID} = :playlistId " +
                "AND `${PlaylistSongEntity.Columns.INDEX}` = :position"
    )
    protected abstract fun at(playlistId: Long, position: Int): PlaylistSongEntity?

    @Transaction
    open fun removeItem(playlistId: Long, songId: Long, position: Int): Boolean {
        val targetEntity = at(playlistId, position)
        return if (targetEntity != null && targetEntity.mediastoreId == songId) {
            delete(targetEntity)
            val max = maximumIndexOf(playlistId)
            if (max > position) for (i in position + 1..max) {
                val entity = at(playlistId, i)
                if (entity != null) update(entity.copy(index = entity.index - 1))
            }
            true
        } else {
            false
        }
    }

    @Transaction
    open fun move(playlistId: Long, from: Int, to: Int): Boolean {
        if (from == to) return true
        val targetEntity = at(playlistId, from)
        return if (targetEntity != null) {
            val songEntity = targetEntity.copy(index = to)

            val range = if (from < to) from + 1 until to else to + 1 until from
            val delta = if (from < to) -1 else +1
            for (position in range) {
                val entity = at(playlistId, position)
                if (entity != null) update(entity.copy(index = entity.index + delta))
            }

            update(songEntity.copy(index = to))
            true
        } else {
            false
        }
    }

    @Transaction
    open fun swap(playlistId: Long, positionA: Int, positionB: Int): Boolean {
        val a = at(playlistId, positionA)
        val b = at(playlistId, positionB)
        return if (a != null && b != null) {
            update(a.copy(index = positionB))
            update(b.copy(index = positionA))
            true
        } else {
            false
        }
    }

    @Insert
    abstract fun insert(playlist: PlaylistSongEntity): Long

    @Insert
    abstract fun insert(playlist: Collection<PlaylistSongEntity>): List<Long>

    @Update
    abstract fun update(playlist: PlaylistSongEntity)

    @Delete
    abstract fun delete(playlist: PlaylistSongEntity): Int

}