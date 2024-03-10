/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.PlaylistEntity
import player.phonograph.repo.room.entity.PlaylistMediastoreSongEntity
import player.phonograph.repo.room.entity.PlaylistSongEntity
import player.phonograph.repo.room.entity.PlaylistWithSongsEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class PlaylistSongDao {

    @Transaction
    @Query("SELECT * FROM ${PlaylistEntity.TABLE_NAME} WHERE ${PlaylistEntity.Columns.ID} =:id")
    abstract fun playlist(id: Long): PlaylistWithSongsEntity?


    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM ${MediastoreSongEntity.TABLE_NAME} " +
            "INNER JOIN ${PlaylistSongEntity.TABLE_NAME} " +
            "ON ${MediastoreSongEntity.TABLE_NAME}.${PlaylistSongEntity.Columns.MEDIASTORE_ID} = ${PlaylistSongEntity.TABLE_NAME}.${MediastoreSongEntity.Columns.ID} " +
            "WHERE ${PlaylistSongEntity.Columns.PLAYLIST_ID} =:playlistId " +
            "ORDER BY ${PlaylistSongEntity.Columns.POSITION} ASC")
    abstract fun songs(playlistId: Long): List<PlaylistMediastoreSongEntity?>

    @Query("SELECT * FROM ${PlaylistSongEntity.TABLE_NAME} WHERE ${PlaylistSongEntity.Columns.PLAYLIST_ID} =:id")
    abstract fun rawQuery(id: Long): List<PlaylistSongEntity>

    @Query(
        "SELECT COALESCE(COUNT(${PlaylistSongEntity.Columns.ID}), 0) " +
                "FROM ${PlaylistSongEntity.TABLE_NAME} " +
                "WHERE ${PlaylistSongEntity.Columns.PLAYLIST_ID} = :playlistId"
    )
    abstract fun size(playlistId: Long): Int

    @Query(
        "SELECT COALESCE(MAX(${PlaylistSongEntity.Columns.POSITION}), -1) " +
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
                "AND ${PlaylistSongEntity.Columns.POSITION} = :position"
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
                if (entity != null) update(entity.copy(position = entity.position - 1))
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
            val songEntity = targetEntity.copy(position = to)

            val range = if (from < to) from + 1 until to else to + 1 until from
            val delta = if (from < to) -1 else +1
            for (position in range) {
                val entity = at(playlistId, position)
                if (entity != null) update(entity.copy(position = entity.position + delta))
            }

            update(songEntity.copy(position = to))
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
            update(a.copy(position = positionB))
            update(b.copy(position = positionA))
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