/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ArtistWithSongs(
    @Embedded var artist: Artist,
    @Relation(
        parentColumn = "artist_id",
        entityColumn = "song_id",
        associateBy = Junction(SongAndArtistLinkage::class)
    )
    var songs: List<Song>
)