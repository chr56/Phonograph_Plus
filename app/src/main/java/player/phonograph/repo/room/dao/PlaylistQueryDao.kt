/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns.PLAYLIST_ID
import player.phonograph.repo.room.entity.PlaylistEntity
import player.phonograph.repo.room.entity.Tables.PLAYLISTS
import androidx.room.Dao
import androidx.room.Query

@Dao
abstract class PlaylistQueryDao {

    @Query("SELECT * from $PLAYLISTS")
    abstract suspend fun all(): List<PlaylistEntity>

    @Query("SELECT * from $PLAYLISTS where $PLAYLIST_ID = :id")
    abstract suspend fun id(id: Long): PlaylistEntity?
}
