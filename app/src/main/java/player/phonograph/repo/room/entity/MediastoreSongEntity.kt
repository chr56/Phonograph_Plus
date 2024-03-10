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
    tableName = MediastoreSongEntity.TABLE_NAME,
    primaryKeys = [MediastoreSongEntity.Columns.ID],
    indices = [Index(
        value = [MediastoreSongEntity.Columns.ID, MediastoreSongEntity.Columns.PATH, MediastoreSongEntity.Columns.TITLE]
    )]
)
data class MediastoreSongEntity(
    @ColumnInfo(name = Columns.ID) var mediastorId: Long = 0,
    @ColumnInfo(name = Columns.PATH) var path: String = "",
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
) {

    companion object {
        const val TABLE_NAME = "mediastore"
    }

    object Columns {
        const val ID = "_id"
        const val PATH = "_data"
        const val DURATION = "duration"
        const val DATE_ADDED = "date_added"
        const val DATE_MODIFIED = "date_modified"
        const val TITLE = "title"
        const val ALBUM_ID = "album_id"
        const val ALBUM = "album"
        const val ARTIST_ID = "artist_id"
        const val ARTIST = "artist"
        const val YEAR = "year"
        const val TRACK = "track"
        const val ALBUM_ARTIST = "album_artist"
        const val COMPOSER = "composer"
    }
}