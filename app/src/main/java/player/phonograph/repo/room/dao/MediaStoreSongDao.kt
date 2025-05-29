/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.model.sort.SortMode
import player.phonograph.repo.mediastore.internal.mediastoreQuerySortOrder
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.Tables
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
abstract class MediaStoreSongDao {

    suspend fun all(sortMode: SortMode): List<MediastoreSongEntity> = rawQuery(
        SimpleSQLiteQuery(
            "SELECT * from ${Tables.MEDIASTORE_SONGS} order by ${sortMode.mediastoreQuerySortOrder()}", // no risks of injection
        )
    )

    @RawQuery
    protected abstract suspend fun rawQuery(query: SupportSQLiteQuery): List<MediastoreSongEntity>

    @Query("SELECT * from ${Tables.MEDIASTORE_SONGS} where ${Columns.MEDIASTORE_ID} = :id")
    abstract suspend fun id(id: Long): MediastoreSongEntity?
    @Query("SELECT * from ${Tables.MEDIASTORE_SONGS} where ${Columns.TITLE} = :title")
    abstract suspend fun title(title: String): MediastoreSongEntity?
    @Query("SELECT * from ${Tables.MEDIASTORE_SONGS} where ${Columns.MEDIASTORE_PATH} like :path")
    abstract suspend fun path(path: String): MediastoreSongEntity?

    suspend fun since(time: Long, useModifiedDate: Boolean): List<MediastoreSongEntity> = rawQuery(run {
        val ref = refOfDate(useModifiedDate)
        SimpleSQLiteQuery(
            "SELECT * from ${Tables.MEDIASTORE_SONGS} where $ref > ? order by $ref DESC", // no risks of injection
            arrayOf(time)
        )
    })

    private fun refOfDate(useModifiedDate: Boolean): String =
        if (useModifiedDate) Columns.DATE_MODIFIED else Columns.DATE_ADDED


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(song: MediastoreSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(songs: Collection<MediastoreSongEntity>)

    @Delete
    abstract suspend fun delete(song: MediastoreSongEntity)
    @Delete
    abstract suspend fun delete(songs: Collection<MediastoreSongEntity>)

    @Query("DELETE FROM ${Tables.MEDIASTORE_SONGS}")
    abstract suspend fun deleteAll()

}
