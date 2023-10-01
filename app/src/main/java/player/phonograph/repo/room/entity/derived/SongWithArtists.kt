/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity.derived

import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.MediastoreSongEntity
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class SongWithArtists(
    @Embedded var songEntity: MediastoreSongEntity,
    @Relation(
        parentColumn = Columns.MEDIASTORE_ID,
        entityColumn = Columns.ARTIST_ID,
        associateBy = Junction(LinkageSongAndArtist::class)
    )
    var artist: List<ArtistEntity>,
)