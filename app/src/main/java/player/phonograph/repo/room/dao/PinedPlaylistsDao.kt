/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.PinedPlaylistsEntity
import player.phonograph.repo.room.entity.Tables.PINED_PLAYLISTS
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class PinedPlaylistsDao {

    @Query("SELECT * FROM $PINED_PLAYLISTS ORDER BY ${Columns.DATE_ADDED} DESC")
    abstract suspend fun all(): List<PinedPlaylistsEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM $PINED_PLAYLISTS WHERE ${Columns.TYPE} = :type AND (${Columns.SUB_ID} = :subId OR ${Columns.LOCATION} = :path))")
    abstract suspend fun contains(type: Int, subId: Long, path: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun add(playlist: PinedPlaylistsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun add(playlists: Collection<PinedPlaylistsEntity>): LongArray

    @Query("DELETE FROM $PINED_PLAYLISTS WHERE ${Columns.PRIMARY_ID} = :id ")
    abstract suspend fun remove(id: Long): Int

    @Query("DELETE FROM $PINED_PLAYLISTS WHERE ${Columns.TYPE} = :type AND ${Columns.SUB_ID} = :subId AND ${Columns.LOCATION} = :path")
    abstract suspend fun remove(type: Int, subId: Long, path: String): Int

    @Query("DELETE FROM $PINED_PLAYLISTS")
    abstract suspend fun purge(): Int
}