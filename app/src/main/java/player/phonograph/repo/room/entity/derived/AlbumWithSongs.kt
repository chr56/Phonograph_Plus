/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity.derived

import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.MediastoreSongEntity
import androidx.room.Embedded
import androidx.room.Relation

data class AlbumWithSongs(
    @Embedded var album: AlbumEntity,
    @Relation(
        parentColumn = Columns.ALBUM_ID,
        entityColumn = Columns.ALBUM_ID
    )
    var songEntities: List<MediastoreSongEntity>,
)