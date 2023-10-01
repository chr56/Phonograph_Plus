/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.model.sort.SortMode
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Columns.ALBUM_NAME
import player.phonograph.repo.room.entity.Tables.ALBUMS
import player.phonograph.repo.room.roomQuerySortOrder
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
abstract class AlbumDao {

    fun all(sortMode: SortMode): List<AlbumEntity> = allRaw(
        SimpleSQLiteQuery(
            "SELECT * from $ALBUMS order by ${sortMode.roomQuerySortOrder()}"// no risks of injection
        )
    )

    @RawQuery
    protected abstract fun allRaw(query: SupportSQLiteQuery): List<AlbumEntity>

    @Query("SELECT * from $ALBUMS where $ALBUM_ID = :id")
    abstract fun id(id: Long): AlbumEntity?

    @Query("SELECT * from $ALBUMS where $ALBUM_NAME = :name")
    abstract fun named(name: String): AlbumEntity?

}
