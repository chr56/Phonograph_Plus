/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.SONG_ID
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ArtistWithAll(
    @Embedded var artist: ArtistEntity,
    @Relation(
        parentColumn = ARTIST_ID,
        entityColumn = ALBUM_ID,
        associateBy = Junction(LinkageAlbumAndArtist::class)
    )
    var albumEntities: List<AlbumEntity>,
    @Relation(
        parentColumn = ARTIST_ID,
        entityColumn = SONG_ID,
        associateBy = Junction(LinkageSongAndArtist::class)
    )
    var songEntities: List<SongEntity>,
)