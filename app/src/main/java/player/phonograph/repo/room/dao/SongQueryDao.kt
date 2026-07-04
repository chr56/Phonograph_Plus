/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.model.sort.SortMode
import player.phonograph.repo.room.dao.RoomSortOrder.roomSongQuerySortOrder
import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.Tables
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
abstract class SongQueryDao {

    suspend fun all(): List<MediastoreSongEntity> = query(
        SimpleSQLiteQuery(
            "SELECT * from ${Tables.MEDIASTORE_SONGS}",
        )
    )

    suspend fun all(sortMode: SortMode): List<MediastoreSongEntity> = query(
        SimpleSQLiteQuery(
            "SELECT * from ${Tables.MEDIASTORE_SONGS} order by ${roomSongQuerySortOrder(sortMode)}",
        )
    )

    @Query("SELECT * from ${Tables.MEDIASTORE_SONGS} where ${Columns.MEDIASTORE_ID} = :id")
    abstract suspend fun id(id: Long): MediastoreSongEntity?

    @Query("SELECT * from ${Tables.MEDIASTORE_SONGS} where ${Columns.MEDIASTORE_ID} in (:ids)")
    abstract suspend fun ids(ids: Collection<Long>): List<MediastoreSongEntity>

    @Query("SELECT ${Columns.MEDIASTORE_ID} from ${Tables.MEDIASTORE_SONGS}")
    abstract suspend fun allIds(): List<Long>

    @Query("SELECT * from ${Tables.MEDIASTORE_SONGS} where ${Columns.TITLE} = :title")
    abstract suspend fun title(title: String): MediastoreSongEntity?

    @Query("SELECT * from ${Tables.MEDIASTORE_SONGS} where ${Columns.MEDIASTORE_PATH} like :path")
    abstract suspend fun path(path: String): MediastoreSongEntity?

    @Query("SELECT * from ${Tables.MEDIASTORE_SONGS} where ${Columns.TITLE} like :title")
    abstract suspend fun searchByTitle(title: String): List<MediastoreSongEntity>

    @Query("SELECT * from ${Tables.MEDIASTORE_SONGS} where ${Columns.MEDIASTORE_PATH} like :path")
    abstract suspend fun searchByPath(path: String): List<MediastoreSongEntity>

    @Query("SELECT COUNT(*) from ${Tables.MEDIASTORE_SONGS}")
    abstract suspend fun total(): Int

    @Query("SELECT * from ${Tables.MEDIASTORE_SONGS} order by ${Columns.DATE_MODIFIED} DESC limit 1")
    abstract suspend fun latest(): MediastoreSongEntity?

    suspend fun since(time: Long, useModifiedDate: Boolean): List<MediastoreSongEntity> = query(run {
        val ref = refOfDate(useModifiedDate)
        SimpleSQLiteQuery(
            "SELECT * from ${Tables.MEDIASTORE_SONGS} where $ref > ? order by $ref DESC",
            arrayOf(time)
        )
    })

    private fun refOfDate(useModifiedDate: Boolean): String =
        if (useModifiedDate) Columns.DATE_MODIFIED else Columns.DATE_ADDED

    //region Raw
    @RawQuery
    protected abstract suspend fun query(query: SupportSQLiteQuery): List<MediastoreSongEntity>
    //endregion
}
