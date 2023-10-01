/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = Tables.LINKAGE_ARTIST_ALBUM,
    primaryKeys = [Columns.ARTIST_ID, Columns.ALBUM_ID],
    indices = [Index(value = [Columns.ALBUM_ID, Columns.ARTIST_ID])]
)
class LinkageAlbumAndArtist(
    @ColumnInfo(name = Columns.ALBUM_ID)
    var albumId: Long,
    @ColumnInfo(name = Columns.ARTIST_ID)
    var artistId: Long,
)