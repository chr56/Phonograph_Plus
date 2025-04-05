/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = Metadata.TABLE_NAME,
    primaryKeys = [Metadata.KEY]
)
data class Metadata(
    @ColumnInfo(name = KEY) val key: String,
    @ColumnInfo(name = VALUE) val value: String,
) {
    companion object Constants {
        const val TABLE_NAME = "metadata"
        const val KEY = "key"
        const val VALUE = "value"
    }
}