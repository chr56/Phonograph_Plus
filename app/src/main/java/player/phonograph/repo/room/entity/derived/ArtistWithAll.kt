/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity.derived

import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.LinkageAlbumAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.MediastoreSongEntity
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ArtistWithAll(
    @Embedded var artist: ArtistEntity,
    @Relation(
        parentColumn = Columns.ARTIST_ID,
        entityColumn = Columns.ALBUM_ID,
        associateBy = Junction(LinkageAlbumAndArtist::class)
    )
    var albumEntities: List<AlbumEntity>,
    @Relation(
        parentColumn = Columns.ARTIST_ID,
        entityColumn = Columns.MEDIASTORE_ID,
        associateBy = Junction(LinkageSongAndArtist::class)
    )
    var songEntities: List<MediastoreSongEntity>,
)