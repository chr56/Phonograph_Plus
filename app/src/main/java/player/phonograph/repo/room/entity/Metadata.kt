/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = Tables.METADATA,
    primaryKeys = [Columns.METADATA_KEY]
)
data class Metadata(
    @ColumnInfo(name = Columns.METADATA_KEY) val key: String,
    @ColumnInfo(name = Columns.METADATA_VALUE) val value: String,
)