/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = Tables.LINKAGE_GENRE_SONG,
    primaryKeys = [Columns.GENRE_ID, Columns.MEDIASTORE_ID],
    indices = [Index(value = [Columns.GENRE_ID, Columns.MEDIASTORE_ID])]
)
data class LinkageGenreAndSong(
    @ColumnInfo(name = Columns.GENRE_ID)
    var genreId: Long,
    @ColumnInfo(name = Columns.MEDIASTORE_ID)
    var songId: Long,
    @ColumnInfo(name = Columns.SOURCE)
    var source: String? = null,
)