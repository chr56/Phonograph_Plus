/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AlbumWithSongs(
    @Embedded var album: Album,
    @Relation(
        parentColumn = "album_id",
        entityColumn = "album_id"
    )
    var songs: List<Song>
)