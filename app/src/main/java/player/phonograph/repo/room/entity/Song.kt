/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "songs",
    indices = [Index(value = ["song_id", "path", "title"])]
)
data class Song(
    @PrimaryKey
    @ColumnInfo(name = "song_id")
    var id: Long, // media store id
    var path: String,
    var size: Long,

    @ColumnInfo(name = "display_name")
    var displayName: String? = null,
    @ColumnInfo(name = "date_added")
    var dateAdded: Long = 0,
    @ColumnInfo(name = "date_modified")
    var dateModified: Long = 0,

    var title: String? = null,

    @ColumnInfo(name = "album_id")
    var albumId: Long = 0,
    @ColumnInfo(name = "album_name")
    var albumName: String? = null,
    @ColumnInfo(name = "artist_id")
    var artistId: Long = 0,
    @ColumnInfo(name = "artist_name")
    var artistName: String? = null,
    @ColumnInfo(name = "album_artist_name")
    var albumArtistName: String?,
    var composer: String?,

    var year: Int = 0,
    var duration: Long = 0,
    @ColumnInfo(name = "track_number")
    var trackNumber: Int = 0,
)