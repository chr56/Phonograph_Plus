/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = Tables.PINED_PLAYLISTS,
    primaryKeys = [Columns.PRIMARY_ID, Columns.TYPE],
    indices = [
        Index(value = [Columns.PRIMARY_ID, Columns.TYPE]),
        Index(value = [Columns.SUB_ID, Columns.LOCATION]),
    ]
)
data class PinedPlaylistsEntity(
    @ColumnInfo(name = Columns.PRIMARY_ID) val id: Long,
    @ColumnInfo(name = Columns.TYPE) val type: Int,
    @ColumnInfo(name = Columns.SUB_ID) val sub: Long,
    @ColumnInfo(name = Columns.LOCATION) val data: String,
    @ColumnInfo(name = Columns.TITLE) val title: String,
    @ColumnInfo(name = Columns.DATE_ADDED) val date: Long,
 ) {
    companion object {
        const val TYPE_FILE_PLAYLIST = 0
        const val TYPE_DATABASE_PLAYLIST = 1
    }
}