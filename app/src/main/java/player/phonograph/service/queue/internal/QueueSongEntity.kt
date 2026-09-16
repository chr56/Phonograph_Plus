/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.service.queue.internal

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val QUEUE_SONGS_TABLE = "queue_songs"
const val QUEUE_SONG_ID_COLUMN = "queue_song_id"
const val QUEUE_TYPE_COLUMN = "queue_type"
const val ORDER_IN_QUEUE_COLUMN = "order_in_queue"
const val MEDIASTORE_SONG_ID_COLUMN = "media_store_id"
const val SONG_TITLE_COLUMN = "title"
const val SONG_TRACK_COLUMN = "track"
const val SONG_YEAR_COLUMN = "year"
const val SONG_DURATION_COLUMN = "duration"
const val SONG_DATA_COLUMN = "_data"
const val SONG_DATE_ADDED_COLUMN = "date_added"
const val SONG_DATE_MODIFIED_COLUMN = "date_modified"
const val SONG_ALBUM_ID_COLUMN = "album_id"
const val SONG_ALBUM_COLUMN = "album"
const val SONG_ARTIST_ID_COLUMN = "artist_id"
const val SONG_ARTIST_COLUMN = "artist"
const val SONG_ALBUM_ARTIST_COLUMN = "album_artist"
const val SONG_COMPOSER_COLUMN = "composer"

const val PLAYING_QUEUE_TYPE = 0
const val ORIGINAL_QUEUE_TYPE = 1

val QUEUE_SONG_PROJECTION = arrayOf(
    MEDIASTORE_SONG_ID_COLUMN,
    SONG_TITLE_COLUMN,
    SONG_TRACK_COLUMN,
    SONG_YEAR_COLUMN,
    SONG_DURATION_COLUMN,
    SONG_DATA_COLUMN,
    SONG_DATE_ADDED_COLUMN,
    SONG_DATE_MODIFIED_COLUMN,
    SONG_ALBUM_ID_COLUMN,
    SONG_ALBUM_COLUMN,
    SONG_ARTIST_ID_COLUMN,
    SONG_ARTIST_COLUMN,
    SONG_ALBUM_ARTIST_COLUMN,
    SONG_COMPOSER_COLUMN,
)

@Entity(
    tableName = QUEUE_SONGS_TABLE,
    indices = [
        Index(
            value = [QUEUE_TYPE_COLUMN, ORDER_IN_QUEUE_COLUMN],
            unique = true,
        ),
    ],
)
data class QueueSongEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = QUEUE_SONG_ID_COLUMN)
    val id: Long = 0,

    @ColumnInfo(name = QUEUE_TYPE_COLUMN)
    val queueType: Int,

    @ColumnInfo(name = ORDER_IN_QUEUE_COLUMN)
    val orderInQueue: Int,

    @ColumnInfo(name = MEDIASTORE_SONG_ID_COLUMN)
    val mediastoreId: Long,

    @ColumnInfo(name = SONG_TITLE_COLUMN)
    val title: String,

    @ColumnInfo(name = SONG_TRACK_COLUMN)
    val trackNumber: Int,

    @ColumnInfo(name = SONG_YEAR_COLUMN)
    val year: Int,

    @ColumnInfo(name = SONG_DURATION_COLUMN)
    val duration: Long,

    @ColumnInfo(name = SONG_DATA_COLUMN)
    val data: String,

    @ColumnInfo(name = SONG_DATE_ADDED_COLUMN)
    val dateAdded: Long,

    @ColumnInfo(name = SONG_DATE_MODIFIED_COLUMN)
    val dateModified: Long,

    @ColumnInfo(name = SONG_ALBUM_ID_COLUMN)
    val albumId: Long,

    @ColumnInfo(name = SONG_ALBUM_COLUMN)
    val albumName: String?,

    @ColumnInfo(name = SONG_ARTIST_ID_COLUMN)
    val artistId: Long,

    @ColumnInfo(name = SONG_ARTIST_COLUMN)
    val artistName: String?,

    @ColumnInfo(name = SONG_ALBUM_ARTIST_COLUMN)
    val albumArtistName: String?,

    @ColumnInfo(name = SONG_COMPOSER_COLUMN)
    val composer: String?,
)