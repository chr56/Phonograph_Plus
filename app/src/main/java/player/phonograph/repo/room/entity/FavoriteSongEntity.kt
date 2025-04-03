/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = FavoriteSongEntity.Columns.TABLE_NAME,
    primaryKeys = [FavoriteSongEntity.Columns.ID],
    indices = [
        Index(value = [FavoriteSongEntity.Columns.ID]),
        Index(value = [FavoriteSongEntity.Columns.PATH]),
    ]
)
data class FavoriteSongEntity(
    @ColumnInfo(name = ID) val mediastoreId: Long,
    @ColumnInfo(name = PATH) val path: String,
    @ColumnInfo(name = TITLE) val title: String,
    @ColumnInfo(name = DATE_ADDED) val date: Long,
) {
    companion object Columns {
        const val TABLE_NAME = "favorite_songs"

        const val ID = "_id"
        const val PATH = "path"
        const val TITLE = "title"
        const val DATE_ADDED = "date_added"
    }
}
