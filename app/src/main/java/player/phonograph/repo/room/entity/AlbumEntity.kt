/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = Tables.ALBUMS,
    primaryKeys = [Columns.ALBUM_ID],
    indices = [Index(Columns.ALBUM_ID, Columns.ALBUM)]
)
data class AlbumEntity(
    @ColumnInfo(name = Columns.ALBUM_ID)
    var albumId: Long,
    @ColumnInfo(name = Columns.ALBUM, defaultValue = "Unnamed Album")
    var albumName: String,
    @ColumnInfo(name = Columns.ARTIST_ID)
    val artistId: Long,
    @ColumnInfo(name = Columns.ALBUM_ARTIST, defaultValue = "Unknown Artist")
    var albumArtistName: String,
    @ColumnInfo(name = Columns.YEAR)
    val year: Int,
    @ColumnInfo(name = Columns.DATE_MODIFIED)
    val dateModified: Long,
    @ColumnInfo(name = Columns.SONG_COUNT, defaultValue = "0")
    val songCount: Int = 0,
)

