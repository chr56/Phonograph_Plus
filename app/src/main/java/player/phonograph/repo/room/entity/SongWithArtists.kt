/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class SongWithArtists(
    @Embedded var song: Song,
    @Relation(
        parentColumn = "song_id",
        entityColumn = "artist_id",
        associateBy = Junction(SongAndArtistLinkage::class)
    )
    var artist: List<Artist>
)