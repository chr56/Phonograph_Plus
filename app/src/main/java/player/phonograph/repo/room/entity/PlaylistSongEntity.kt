/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.entity

import player.phonograph.repo.room.entity.Columns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = Tables.PLAYLIST_SONGS,
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = [Columns.PLAYLIST_ID],
            childColumns = [Columns.PLAYLIST_ID],
            onDelete = ForeignKey.CASCADE,
            deferred = true,
        ),
    ],
    indices = [
        Index(
            value = [
                Columns.PLAYLIST_ID,
                Columns.MEDIASTORE_ID,
            ]
        ),
        Index(
            value = [
                Columns.MEDIASTORE_ID,
                Columns.PLAYLIST_ID,
            ]
        ),
    ]
)
data class PlaylistSongEntity(

    @ColumnInfo(name = Columns.PLAYLIST_SONG_ID, typeAffinity = ColumnInfo.INTEGER)
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = Columns.MEDIASTORE_ID, defaultValue = "0")
    val mediastoreId: Long = 0,

    @ColumnInfo(name = Columns.PLAYLIST_ID, defaultValue = "0")
    val playlistId: Long = 0,

    @ColumnInfo(name = Columns.POSITION)
    val position: Int,

    @ColumnInfo(name = Columns.PATH)
    val path: String,

    ) {

    companion object {

        fun from(
            playlistEntity: PlaylistEntity,
            mediastoreSongEntity: MediastoreSongEntity,
            index: Int,
        ): PlaylistSongEntity = PlaylistSongEntity(
            mediastoreId = mediastoreSongEntity.mediastorId,
            playlistId = playlistEntity.id,
            position = index,
            path = mediastoreSongEntity.path
        )
    }
}