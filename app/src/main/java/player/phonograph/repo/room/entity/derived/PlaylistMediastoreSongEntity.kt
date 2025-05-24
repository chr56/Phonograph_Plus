/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity.derived

import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.MediastoreSongEntity
import androidx.room.ColumnInfo
import androidx.room.Embedded

data class PlaylistMediastoreSongEntity(
    @Embedded
    val songEntity: MediastoreSongEntity,
    @ColumnInfo(Columns.POSITION)
    val position: Int,
    @ColumnInfo(Columns.PLAYLIST_ID)
    val playlistId: Long,
)