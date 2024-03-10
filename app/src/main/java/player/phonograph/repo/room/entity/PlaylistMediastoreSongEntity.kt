/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class PlaylistMediastoreSongEntity(
    @Embedded
    val songEntity: MediastoreSongEntity,
    @ColumnInfo(PlaylistSongEntity.Columns.POSITION)
    val position: Int,
    @ColumnInfo(PlaylistSongEntity.Columns.PLAYLIST_ID)
    val playlistId: Long,
)