/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.ARTIST_NAME
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = Tables.ARTISTS,
    primaryKeys = [ARTIST_ID, ARTIST_NAME]
)
data class Artist(
    @ColumnInfo(name = ARTIST_ID)
    var artistId: Long = 0,
    @ColumnInfo(name = ARTIST_NAME)
    var artistName: String,
)

