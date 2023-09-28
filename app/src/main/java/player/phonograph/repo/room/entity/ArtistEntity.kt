/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import player.phonograph.repo.room.entity.Columns.ALBUM_COUNT
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.ARTIST_NAME
import player.phonograph.repo.room.entity.Columns.SONG_COUNT
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = Tables.ARTISTS,
    primaryKeys = [ARTIST_ID, ARTIST_NAME]
)
data class ArtistEntity(
    @ColumnInfo(name = ARTIST_ID)
    var artistId: Long = 0,
    @ColumnInfo(name = ARTIST_NAME)
    var artistName: String,
    @ColumnInfo(name = ALBUM_COUNT, defaultValue = "0")
    val albumCount: Int = 0,
    @ColumnInfo(name = SONG_COUNT, defaultValue = "0")
    val songCount: Int = 0,
)

