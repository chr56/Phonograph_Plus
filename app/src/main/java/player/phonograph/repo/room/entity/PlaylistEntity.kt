/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(
    tableName = Tables.PLAYLISTS
)
data class PlaylistEntity(
    @ColumnInfo(name = Columns.PLAYLIST_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = Columns.PLAYLIST_NAME)
    val name: String = "",
    @ColumnInfo(name = Columns.DATE_ADDED)
    val dateAdded: Long = 0,
    @ColumnInfo(name = Columns.DATE_MODIFIED)
    val dateModified: Long = 0,
)