/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.Tables.ALBUMS
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class AlbumManipulateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(album: AlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(albums: List<AlbumEntity>): LongArray

    suspend fun updateCounter(queryDao: AlbumQueryDao, albumId: Long, songCount: Int): Boolean {
        val original = queryDao.id(albumId) ?: return false
        val updated = original.copy(songCount = songCount)
        return update(updated) == albumId
    }

    suspend fun delete(queryDao: AlbumQueryDao, albumId: Long): Boolean {
        return delete(queryDao.id(albumId) ?: return false) > 0
    }

    @Delete
    abstract suspend fun delete(album: AlbumEntity): Int

    @Delete
    abstract suspend fun delete(albums: List<AlbumEntity>): Int

    @Query("DELETE FROM $ALBUMS")
    abstract suspend fun deleteAll(): Int
}
