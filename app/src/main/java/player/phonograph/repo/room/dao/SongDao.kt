/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.model.sort.SortMode
import player.phonograph.repo.room.Converters
import player.phonograph.repo.room.entity.Columns.PATH
import player.phonograph.repo.room.entity.Columns.SONG_ID
import player.phonograph.repo.room.entity.Columns.TITLE
import player.phonograph.repo.room.entity.SongEntity
import player.phonograph.repo.room.entity.Tables.SONGS
import player.phonograph.repo.room.refOfDate
import player.phonograph.repo.room.roomQuerySortOrder
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
@TypeConverters(Converters::class)
abstract class SongDao {

    fun all(sortMode: SortMode): List<SongEntity> = allRaw(
        SimpleSQLiteQuery(
            "SELECT * from $SONGS order by ${sortMode.roomQuerySortOrder()}", // no risks of injection
        )
    )

    @RawQuery
    protected abstract fun allRaw(query: SupportSQLiteQuery): List<SongEntity>

    @Query("SELECT * from $SONGS where $SONG_ID = :id")
    abstract fun id(id: Long): SongEntity?
    @Query("SELECT * from $SONGS where $TITLE = :title")
    abstract fun title(title: String): SongEntity?
    @Query("SELECT * from $SONGS where $PATH like :path")
    abstract fun path(path: String): SongEntity?

    fun since(time: Long, useModifiedDate: Boolean): List<SongEntity> = sinceRaw(
        run {
            val ref = refOfDate(useModifiedDate)
            SimpleSQLiteQuery(
                "SELECT * from $SONGS where $ref > ? order by $ref DESC", // no risks of injection
                arrayOf(time)
            )
        }
    )

    @RawQuery
    protected abstract fun sinceRaw(query: SupportSQLiteQuery): List<SongEntity>

}
