/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import androidx.room.Embedded
import androidx.room.Relation

data class AlbumWithSongs(
    @Embedded var album: AlbumEntity,
    @Relation(
        parentColumn = ALBUM_ID,
        entityColumn = ALBUM_ID
    )
    var songEntities: List<SongEntity>,
)