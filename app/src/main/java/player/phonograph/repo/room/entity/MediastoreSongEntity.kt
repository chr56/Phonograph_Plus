/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.entity


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index


/**
 * Cache table of MediaStore
 */
@Entity(
    tableName = Tables.MEDIASTORE_SONGS,
    primaryKeys = [Columns.MEDIASTORE_ID],
    indices = [Index(value = [Columns.MEDIASTORE_ID, Columns.MEDIASTORE_PATH, Columns.TITLE])]
)
data class MediastoreSongEntity(
    @ColumnInfo(name = Columns.MEDIASTORE_ID) var mediastorId: Long = 0,
    @ColumnInfo(name = Columns.MEDIASTORE_PATH) var path: String = "",
    @ColumnInfo(name = Columns.DURATION) var duration: Long = -1,
    @ColumnInfo(name = Columns.DATE_ADDED) var dateAdded: Long = 0,
    @ColumnInfo(name = Columns.DATE_MODIFIED) var dateModified: Long = 0,
    @ColumnInfo(name = Columns.TITLE) var title: String = "",
    @ColumnInfo(name = Columns.ALBUM_ID) var albumId: Long = 0,
    @ColumnInfo(name = Columns.ALBUM) var album: String = "",
    @ColumnInfo(name = Columns.ARTIST_ID) var artistId: Long = 0,
    @ColumnInfo(name = Columns.ARTIST) var artist: String = "",
    @ColumnInfo(name = Columns.YEAR) var year: Int = -1,
    @ColumnInfo(name = Columns.TRACK) var track: Int = -1,
    @ColumnInfo(name = Columns.ALBUM_ARTIST) var albumArtist: String = "",
    @ColumnInfo(name = Columns.COMPOSER) var composer: String = "",
)