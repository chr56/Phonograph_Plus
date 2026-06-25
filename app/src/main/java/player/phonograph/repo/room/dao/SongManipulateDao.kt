/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.Tables
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class SongManipulateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(song: MediastoreSongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(songs: Collection<MediastoreSongEntity>)

    @Delete
    abstract suspend fun delete(song: MediastoreSongEntity)

    @Delete
    abstract suspend fun delete(songs: Collection<MediastoreSongEntity>)

    @Query("DELETE FROM ${Tables.MEDIASTORE_SONGS}")
    abstract suspend fun deleteAll()
}
