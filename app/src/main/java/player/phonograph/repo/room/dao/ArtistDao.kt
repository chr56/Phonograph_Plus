/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.model.sort.SortMode
import player.phonograph.repo.room.dao.RoomSortOrder.roomArtistQuerySortOrder
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.Columns.ARTIST
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Tables.ARTISTS
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
abstract class ArtistDao {

    fun all(sortMode: SortMode): List<ArtistEntity> = allRaw(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS order by ${roomArtistQuerySortOrder(sortMode)}", // no risks of injection
        )
    )

    @RawQuery
    protected abstract fun allRaw(query: SupportSQLiteQuery): List<ArtistEntity>

    @Query("SELECT * from $ARTISTS where $ARTIST_ID = :id")
    abstract fun id(id: Long): ArtistEntity?

    @Query("SELECT * from $ARTISTS where $ARTIST = :name")
    abstract fun named(name: String): ArtistEntity?

    @Query("SELECT COUNT(*) from $ARTISTS")
    abstract fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(artist: ArtistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(artists: List<ArtistEntity>): LongArray

    suspend fun updateCounter(artistId: Long, songCount: Int = -1, albumCount: Int = -1): Boolean {
        val original = id(artistId) ?: return false
        val updated = when {
            songCount > -1 && albumCount > -1 -> original.copy(songCount = songCount, albumCount = albumCount)
            songCount > -1                    -> original.copy(songCount = songCount)
            albumCount > -1                   -> original.copy(albumCount = albumCount)
            else                              -> return false
        }
        return update(updated) == artistId
    }

    suspend fun delete(artistId: Long): Boolean {
        return delete(id(artistId) ?: return false) > 0
    }

    @Delete
    abstract suspend fun delete(artist: ArtistEntity): Int

    @Delete
    abstract suspend fun delete(artists: List<ArtistEntity>): Int

    @Query("DELETE FROM $ARTISTS")
    abstract suspend fun deleteAll(): Int
}