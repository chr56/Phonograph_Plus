/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.model.sort.SortMode
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.ARTIST_NAME
import player.phonograph.repo.room.entity.Tables.ARTISTS
import player.phonograph.repo.room.roomQuerySortOrder
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
abstract class ArtistDao {

    fun all(sortOrder: SortMode): List<ArtistEntity> = allRaw(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS order by ${sortOrder.roomQuerySortOrder()}", // no risks of injection
        )
    )

    @RawQuery
    protected abstract fun allRaw(query: SupportSQLiteQuery): List<ArtistEntity>

    @Query("SELECT * from $ARTISTS where $ARTIST_ID = :id")
    abstract fun id(id: Long): ArtistEntity?

    @Query("SELECT * from $ARTISTS where $ARTIST_NAME = :name")
    abstract fun named(name: String): ArtistEntity?

}