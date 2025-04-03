/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = FavoritePlaylistEntity.Columns.TABLE_NAME,
    primaryKeys = [FavoritePlaylistEntity.Columns.ID, FavoritePlaylistEntity.Columns.TYPE],
    indices = [
        Index(value = [FavoritePlaylistEntity.Columns.ID, FavoritePlaylistEntity.Columns.TYPE]),
        Index(value = [FavoritePlaylistEntity.Columns.SUB_ID, FavoritePlaylistEntity.Columns.DATA]),
    ]
)
data class FavoritePlaylistEntity(
    @ColumnInfo(name = ID) val id: Long,
    @ColumnInfo(name = TYPE) val type: Int,
    @ColumnInfo(name = SUB_ID) val sub: Long,
    @ColumnInfo(name = DATA) val data: String,
    @ColumnInfo(name = TITLE) val title: String,
    @ColumnInfo(name = DATE_ADDED) val date: Long,
) {
    companion object Columns {
        const val TABLE_NAME = "favorite_playlists"

        const val ID = "primary_id"
        const val TYPE = "type"
        const val SUB_ID = "sub_id"
        const val DATA = "location"
        const val TITLE = "title"
        const val DATE_ADDED = "date_added"
    }
}
