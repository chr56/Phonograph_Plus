/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns.LOCATION
import player.phonograph.repo.room.entity.Columns.PRIMARY_ID
import player.phonograph.repo.room.entity.Columns.SUB_ID
import player.phonograph.repo.room.entity.Columns.TYPE
import player.phonograph.repo.room.entity.PinedPlaylistsEntity
import player.phonograph.repo.room.entity.Tables.PINED_PLAYLISTS
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class PinedPlaylistManipulateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun add(playlist: PinedPlaylistsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun add(playlists: Collection<PinedPlaylistsEntity>): LongArray

    @Query("DELETE FROM $PINED_PLAYLISTS where $PRIMARY_ID = :id")
    abstract suspend fun remove(id: Long): Int

    @Query("DELETE FROM $PINED_PLAYLISTS where $TYPE = :type and $SUB_ID = :subId and $LOCATION = :path")
    abstract suspend fun remove(type: Int, subId: Long, path: String): Int

    @Query("DELETE FROM $PINED_PLAYLISTS")
    abstract suspend fun purge(): Int
}
