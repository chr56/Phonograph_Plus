/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.model.sort.SortMode
import player.phonograph.repo.room.dao.RoomSortOrder.roomGenreQuerySortOrder
import player.phonograph.repo.room.entity.Columns.GENRE
import player.phonograph.repo.room.entity.Columns.GENRE_ID
import player.phonograph.repo.room.entity.Columns.GENRE_ID_MEDIASTORE
import player.phonograph.repo.room.entity.GenreEntity
import player.phonograph.repo.room.entity.Tables.GENRES
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
abstract class GenreDao {

    fun all(sortMode: SortMode): List<GenreEntity> = allRaw(
        SimpleSQLiteQuery(
            "SELECT * from $GENRES order by ${roomGenreQuerySortOrder(sortMode)}"// no risks of injection
        )
    )

    @RawQuery
    protected abstract fun allRaw(query: SupportSQLiteQuery): List<GenreEntity>


    @Query("SELECT * from $GENRES where $GENRE_ID = :id")
    abstract fun id(id: Long): GenreEntity?

    @Query("SELECT * from $GENRES where $GENRE = :name")
    abstract fun named(name: String): GenreEntity?

    @Query("SELECT COUNT(*) from $GENRES")
    abstract fun count(): Int

    @Query("SELECT * from $GENRES where $GENRE_ID_MEDIASTORE = :mediastoreId")
    abstract fun mediaStoreId(mediastoreId: Long): GenreEntity?

    @Query("SELECT $GENRE_ID_MEDIASTORE from $GENRES")
    abstract fun allMediaStoreIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(genre: GenreEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(genres: List<GenreEntity>): LongArray

    suspend fun updateCounter(genreId: Long, songCount: Int): Boolean {
        val original = id(genreId) ?: return false
        val updated = original.copy(songCount = songCount)
        return update(updated) == genreId
    }

    suspend fun delete(genreId: Long): Boolean {
        return delete(id(genreId) ?: return false) > 0
    }

    @Delete
    abstract suspend fun delete(genre: GenreEntity): Int

    @Delete
    abstract suspend fun delete(genres: List<GenreEntity>): Int

    @Query("DELETE FROM $GENRES")
    abstract suspend fun deleteAll(): Int

}