/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.entity.derived

import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.PlaylistEntity
import player.phonograph.repo.room.entity.PlaylistSongEntity
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlaylistWithSongsEntity(
    @Embedded
    val playlist: PlaylistEntity,
    @Relation(
        associateBy = Junction(
            value = PlaylistSongEntity::class,
            parentColumn = Columns.PLAYLIST_ID,
            entityColumn = Columns.MEDIASTORE_ID
        ),
        parentColumn = Columns.PLAYLIST_ID,
        entityColumn = Columns.MEDIASTORE_ID
    )
    val songs: List<MediastoreSongEntity?>,
) {
}