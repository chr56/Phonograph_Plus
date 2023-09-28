/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import player.phonograph.repo.room.entity.Columns.ALBUM_ARTIST_NAME
import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Columns.ALBUM_NAME
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.DATE_MODIFIED
import player.phonograph.repo.room.entity.Columns.YEAR
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = Tables.ALBUMS,
    primaryKeys = [ALBUM_ID],
    indices = [Index(ALBUM_ID, ALBUM_NAME)]
)
data class Album(
    @ColumnInfo(name = ALBUM_ID)
    var albumId: Long,
    @ColumnInfo(name = ALBUM_NAME, defaultValue = UNKNOWN_ALBUM_DISPLAY_NAME)
    var albumName: String,
    @ColumnInfo(name = ARTIST_ID)
    val artistId: Long,
    @ColumnInfo(name = ALBUM_ARTIST_NAME, defaultValue = UNKNOWN_ARTIST_DISPLAY_NAME)
    var albumArtistName: String,
    @ColumnInfo(name = YEAR)
    val year: Int,
    @ColumnInfo(name = DATE_MODIFIED)
    val dateModified: Long,
) {
}

