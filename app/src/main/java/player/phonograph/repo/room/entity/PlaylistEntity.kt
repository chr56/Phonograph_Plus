/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(
    tableName = PlaylistEntity.TABLE_NAME
)
data class PlaylistEntity(
    @ColumnInfo(name = Columns.ID)
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = Columns.NAME)
    val name: String = "",
    @ColumnInfo(name = Columns.DATE_ADDED)
    val dateAdded: Long = 0,
    @ColumnInfo(name = Columns.DATE_MODIFIED)
    val dateModified: Long = 0,
) {
    object Columns {
        const val ID = "playlist_id"
        const val NAME = "playlist_name"
        const val DATE_ADDED = "date_added"
        const val DATE_MODIFIED = "date_modified"
    }

    companion object {
        const val TABLE_NAME = "playlists"
    }
}