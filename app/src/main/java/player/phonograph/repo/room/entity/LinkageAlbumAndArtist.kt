/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = Tables.LINKAGE_ARTIST_ALBUM,
    primaryKeys = [ARTIST_ID, ALBUM_ID],
    indices = [Index(value = [ALBUM_ID, ARTIST_ID])]
)
class LinkageAlbumAndArtist(
    @ColumnInfo(name = ALBUM_ID)
    var albumId: Long,
    @ColumnInfo(name = ARTIST_ID)
    var artistId: Long,
)