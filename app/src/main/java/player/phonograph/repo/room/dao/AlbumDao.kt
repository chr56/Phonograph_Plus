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
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
abstract class AlbumDao {

    fun all(sortMode: SortMode): List<AlbumEntity> = allRaw(
        SimpleSQLiteQuery(
            "SELECT * from $ALBUMS order by ${roomAlbumQuerySortOrder(sortMode)}"// no risks of injection
        )
    )

    @RawQuery
    protected abstract fun allRaw(query: SupportSQLiteQuery): List<AlbumEntity>

    @Query("SELECT * from $ALBUMS where $ALBUM_ID = :id")
    abstract fun id(id: Long): AlbumEntity?

    @Query("SELECT * from $ALBUMS where $ALBUM = :name")
    abstract fun named(name: String): AlbumEntity?

    @Query("SELECT COUNT(*) from $ALBUMS")
    abstract fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(album: AlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(albums: List<AlbumEntity>): LongArray

    suspend fun updateCounter(albumId: Long, songCount: Int): Boolean {
        val original = id(albumId) ?: return false
        val updated = original.copy(songCount = songCount)
        return update(updated) == albumId
    }

    suspend fun delete(albumId: Long): Boolean {
        return delete(id(albumId) ?: return false) > 0
    }

    @Delete
    abstract suspend fun delete(album: AlbumEntity): Int

    @Delete
    abstract suspend fun delete(albums: List<AlbumEntity>): Int

    @Query("DELETE FROM $ALBUMS")
    abstract suspend fun deleteAll(): Int

}
