/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.GenreEntity
import player.phonograph.repo.room.entity.Tables.GENRES
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class GenreManipulateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(genre: GenreEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(genres: List<GenreEntity>): LongArray

    suspend fun updateCounter(queryDao: GenreQueryDao, genreId: Long, songCount: Int): Boolean {
        val original = queryDao.id(genreId) ?: return false
        val updated = original.copy(songCount = songCount)
        return update(updated) == genreId
    }

    suspend fun delete(queryDao: GenreQueryDao, genreId: Long): Boolean {
        return delete(queryDao.id(genreId) ?: return false) > 0
    }

    @Delete
    abstract suspend fun delete(genre: GenreEntity): Int

    @Delete
    abstract suspend fun delete(genres: List<GenreEntity>): Int

    @Query("DELETE FROM $GENRES")
    abstract suspend fun deleteAll(): Int
}
