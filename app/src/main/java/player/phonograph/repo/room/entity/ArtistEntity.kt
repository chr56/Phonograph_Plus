/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = Tables.ARTISTS,
    primaryKeys = [Columns.ARTIST_ID, Columns.ARTIST]
)
data class ArtistEntity(
    @ColumnInfo(name = Columns.ARTIST_ID)
    var artistId: Long = 0,
    @ColumnInfo(name = Columns.ARTIST)
    var artistName: String,
    @ColumnInfo(name = Columns.ALBUM_COUNT, defaultValue = "0")
    val albumCount: Int = 0,
    @ColumnInfo(name = Columns.SONG_COUNT, defaultValue = "0")
    val songCount: Int = 0,
)

