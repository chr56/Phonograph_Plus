/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.FavoriteSongEntity
import player.phonograph.repo.room.entity.Tables.FAVORITE_SONGS
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class FavoritesSongsDao {

    @Query("SELECT * FROM $FAVORITE_SONGS ORDER BY ${Columns.DATE_ADDED} DESC")
    abstract suspend fun all(): List<FavoriteSongEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM $FAVORITE_SONGS WHERE ${Columns.MEDIASTORE_ID} = :id OR ${Columns.PATH} = :path)")
    abstract suspend fun contains(id: Long, path: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun add(song: FavoriteSongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun add(songs: Collection<FavoriteSongEntity>): LongArray

    @Query("DELETE FROM $FAVORITE_SONGS WHERE ${Columns.MEDIASTORE_ID} = :id AND ${Columns.PATH} = :path")
    abstract suspend fun remove(id: Long, path: String): Int

    @Query("DELETE FROM $FAVORITE_SONGS")
    abstract suspend fun purge(): Int
}