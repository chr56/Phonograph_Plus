/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.model.sort.SortMode
import player.phonograph.repo.room.dao.RoomSortOrder.roomAlbumQuerySortOrder
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.Columns.ALBUM
import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Tables.ALBUMS
import player.phonograph.repo.room.entity.Tables.MEDIASTORE_SONGS
import player.phonograph.repo.room.entity.derived.AlbumWithSongs
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
abstract class AlbumQueryDao {

    suspend fun all(sortMode: SortMode): List<AlbumEntity> = query(
        SimpleSQLiteQuery(
            "SELECT * from $ALBUMS order by ${roomAlbumQuerySortOrder(sortMode)}"
        )
    )

    @Query("SELECT * from $ALBUMS where $ALBUM_ID = :id")
    abstract suspend fun id(id: Long): AlbumEntity?

    @Query("SELECT * from $ALBUMS where $ALBUM_ID in (:ids)")
    abstract suspend fun ids(ids: Collection<Long>): List<AlbumEntity>

    @Query("SELECT * from $ALBUMS where $ALBUM = :name")
    abstract suspend fun named(name: String): AlbumEntity?

    @Query("SELECT * from $ALBUMS where $ALBUM like :name")
    abstract suspend fun searchByName(name: String): List<AlbumEntity>

    @Query("SELECT COUNT(*) from $ALBUMS")
    abstract suspend fun count(): Int

    @Query("SELECT * from $ALBUMS where $ALBUM_ID = :albumId")
    abstract suspend fun albumSongs(albumId: Long): AlbumWithSongs?

    @Query("SELECT COUNT(*) from $MEDIASTORE_SONGS where $ALBUM_ID = :albumId")
    abstract suspend fun albumSongCount(albumId: Long): Int

    //region Raw
    @RawQuery
    protected abstract suspend fun query(query: SupportSQLiteQuery): List<AlbumEntity>

    @Transaction
    @RawQuery
    protected abstract suspend fun queryAlbumWithSongs(query: SupportSQLiteQuery): AlbumWithSongs?
    //endregion
}
