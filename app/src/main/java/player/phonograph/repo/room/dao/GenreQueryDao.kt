/*
 *  Copyright (c) 2022~2023 chr_56
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
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
abstract class GenreQueryDao {

    suspend fun all(sortMode: SortMode): List<GenreEntity> = query(
        SimpleSQLiteQuery(
            "SELECT * from $GENRES order by ${roomGenreQuerySortOrder(sortMode)}"
        )
    )

    @Query("SELECT * from $GENRES where $GENRE_ID = :id")
    abstract suspend fun id(id: Long): GenreEntity?

    @Query("SELECT * from $GENRES where $GENRE = :name")
    abstract suspend fun named(name: String): GenreEntity?

    @Query("SELECT * from $GENRES where $GENRE like :name")
    abstract suspend fun searchByName(name: String): List<GenreEntity>

    @Query("SELECT COUNT(*) from $GENRES")
    abstract suspend fun count(): Int


    @Query("SELECT * from $GENRES where $GENRE_ID_MEDIASTORE = :mediastoreId")
    abstract suspend fun mediaStoreId(mediastoreId: Long): GenreEntity?

    @Query("SELECT $GENRE_ID_MEDIASTORE from $GENRES")
    abstract suspend fun allMediaStoreIds(): List<Long>

    //region Raw
    @RawQuery
    abstract suspend fun query(query: SupportSQLiteQuery): List<GenreEntity>
    //endregion
}
