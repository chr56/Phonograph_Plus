/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import player.phonograph.repo.room.entity.Columns.ALBUM_ARTIST_NAME
import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Columns.ALBUM_NAME
import player.phonograph.repo.room.entity.Columns.COMPOSER
import player.phonograph.repo.room.entity.Columns.DATE_ADDED
import player.phonograph.repo.room.entity.Columns.DATE_MODIFIED
import player.phonograph.repo.room.entity.Columns.DURATION
import player.phonograph.repo.room.entity.Columns.PATH
import player.phonograph.repo.room.entity.Columns.RAW_ARTIST_ID
import player.phonograph.repo.room.entity.Columns.RAW_ARTIST_NAME
import player.phonograph.repo.room.entity.Columns.SONG_ID
import player.phonograph.repo.room.entity.Columns.TITLE
import player.phonograph.repo.room.entity.Columns.TRACK_NUMBER
import player.phonograph.repo.room.entity.Columns.YEAR
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index


@Entity(
    tableName = Tables.SONGS,
    primaryKeys = [SONG_ID],
    indices = [Index(value = [SONG_ID, PATH, TITLE])]
)
data class SongEntity(
    @ColumnInfo(name = SONG_ID)
    var id: Long, // media store id
    @ColumnInfo(name = PATH)
    var path: String,

    @ColumnInfo(name = DATE_ADDED)
    var dateAdded: Long = 0,
    @ColumnInfo(name = DATE_MODIFIED)
    var dateModified: Long = 0,

    @ColumnInfo(name = TITLE)
    var title: String? = null,

    @ColumnInfo(name = ALBUM_ID)
    var albumId: Long = 0,
    @ColumnInfo(name = ALBUM_NAME)
    var albumName: String?,
    @ColumnInfo(name = RAW_ARTIST_ID)
    var rawArtistId: Long = 0,
    @ColumnInfo(name = RAW_ARTIST_NAME)
    var rawArtistName: String?,
    @ColumnInfo(name = ALBUM_ARTIST_NAME)
    var albumArtistName: String?,
    @ColumnInfo(name = COMPOSER)
    var composer: String?,

    @ColumnInfo(name = YEAR)
    var year: Int = 0,
    @ColumnInfo(name = DURATION)
    var duration: Long = 0,
    @ColumnInfo(name = TRACK_NUMBER)
    var trackNumber: Int = 0,
)