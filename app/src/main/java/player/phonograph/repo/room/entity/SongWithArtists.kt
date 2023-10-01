/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.SONG_ID
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class SongWithArtists(
    @Embedded var songEntity: SongEntity,
    @Relation(
        parentColumn = SONG_ID,
        entityColumn = ARTIST_ID,
        associateBy = Junction(LinkageSongAndArtist::class)
    )
    var artist: List<ArtistEntity>,
)