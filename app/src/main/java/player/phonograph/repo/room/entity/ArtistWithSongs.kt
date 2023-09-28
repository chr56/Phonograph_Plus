/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.SONG_ID
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ArtistWithSongs(
    @Embedded var artist: Artist,
    @Relation(
        parentColumn = ARTIST_ID,
        entityColumn = SONG_ID,
        associateBy = Junction(SongAndArtistLinkage::class)
    )
    var songs: List<Song>,
)