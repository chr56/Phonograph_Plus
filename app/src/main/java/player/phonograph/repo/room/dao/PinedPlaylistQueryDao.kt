/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns.LOCATION
import player.phonograph.repo.room.entity.Columns.SUB_ID
import player.phonograph.repo.room.entity.Columns.TYPE
import player.phonograph.repo.room.entity.PinedPlaylistsEntity
import player.phonograph.repo.room.entity.Tables.PINED_PLAYLISTS
import androidx.room.Dao
import androidx.room.Query

@Dao
abstract class PinedPlaylistQueryDao {

    @Query("SELECT * from $PINED_PLAYLISTS order by DATE_ADDED DESC")
    abstract suspend fun all(): List<PinedPlaylistsEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM $PINED_PLAYLISTS WHERE $TYPE = :type AND ($SUB_ID = :subId OR $LOCATION = :path))")
    abstract suspend fun contains(type: Int, subId: Long, path: String): Boolean
}
