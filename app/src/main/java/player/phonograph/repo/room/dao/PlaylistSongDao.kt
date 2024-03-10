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

    @Insert
    abstract fun insert(playlist: PlaylistSongEntity): Long

    @Insert
    abstract fun insert(playlist: Collection<PlaylistSongEntity>): List<Long>

    @Update
    abstract fun update(playlist: PlaylistSongEntity)

    @Delete
    abstract fun delete(playlist: PlaylistSongEntity): Int

}