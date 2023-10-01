/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ArtistWithAlbums(
    @Embedded var artist: ArtistEntity,
    @Relation(
        parentColumn = ARTIST_ID,
        entityColumn = ALBUM_ID,
        associateBy = Junction(LinkageAlbumAndArtist::class)
    )
    var albumEntities: List<AlbumEntity>,
)