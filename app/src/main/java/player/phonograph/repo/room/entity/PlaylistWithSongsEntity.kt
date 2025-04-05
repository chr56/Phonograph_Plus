/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlaylistWithSongsEntity(
    @Embedded
    val playlist: PlaylistEntity,
    @Relation(
        associateBy = Junction(
            value = PlaylistSongEntity::class,
            parentColumn = PlaylistSongEntity.Columns.PLAYLIST_ID,
            entityColumn = PlaylistSongEntity.Columns.MEDIASTORE_ID
        ),
        parentColumn = PlaylistEntity.Columns.ID,
        entityColumn = MediastoreSongEntity.Columns.ID
    )
    val songs: List<MediastoreSongEntity?>,
) {
}