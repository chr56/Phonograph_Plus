/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = Tables.GENRES,
    indices = [Index(Columns.GENRE_ID, Columns.GENRE)],
)
data class GenreEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Columns.GENRE_ID)
    val id: Long,
    @ColumnInfo(name = Columns.GENRE)
    val name: String,
    @ColumnInfo(name = Columns.CATEGORY)
    val category: String? = null,
    @ColumnInfo(name = Columns.GENRE_ID_MEDIASTORE)
    val mediastoreId: Long = 0,
    @ColumnInfo(name = Columns.SONG_COUNT, defaultValue = "0")
    val songCount: Int = 0,
)