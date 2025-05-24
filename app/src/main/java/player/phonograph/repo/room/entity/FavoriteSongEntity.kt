/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = Tables.FAVORITE_SONGS,
    primaryKeys = [Columns.MEDIASTORE_ID],
    indices = [
        Index(value = [Columns.MEDIASTORE_ID]),
        Index(value = [Columns.PATH]),
    ]
)
data class FavoriteSongEntity(
    @ColumnInfo(name = Columns.MEDIASTORE_ID) val mediastoreId: Long,
    @ColumnInfo(name = Columns.PATH) val path: String,
    @ColumnInfo(name = Columns.TITLE) val title: String,
    @ColumnInfo(name = Columns.DATE_ADDED) val date: Long,
)
