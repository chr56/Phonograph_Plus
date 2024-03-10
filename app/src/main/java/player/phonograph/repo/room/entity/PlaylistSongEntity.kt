/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = PlaylistSongEntity.TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = [PlaylistEntity.Columns.ID],
            childColumns = [PlaylistSongEntity.Columns.PLAYLIST_ID],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediastoreSongEntity::class,
            parentColumns = [MediastoreSongEntity.Columns.ID],
            childColumns = [PlaylistSongEntity.Columns.MEDIASTORE_ID],
        )
    ],
    indices = [
        Index(
            value = [
                PlaylistSongEntity.Columns.PLAYLIST_ID,
                PlaylistSongEntity.Columns.MEDIASTORE_ID,
            ]
        ),
        Index(
            value = [
                PlaylistSongEntity.Columns.MEDIASTORE_ID,
                PlaylistSongEntity.Columns.PLAYLIST_ID,
            ]
        ),
    ]
)
data class PlaylistSongEntity(

    @ColumnInfo(name = Columns.ID, typeAffinity = ColumnInfo.INTEGER)
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = Columns.MEDIASTORE_ID)
    val mediastoreId: Long = 0,

    @ColumnInfo(name = Columns.PLAYLIST_ID)
    val playlistId: Long = 0,

    @ColumnInfo(name = Columns.INDEX)
    val index: Int,

    @ColumnInfo(name = Columns.PATH)
    val path: String,

    ) {
    object Columns {
        const val ID = "playlist_song_id"
        const val PLAYLIST_ID = PlaylistEntity.Columns.ID
        const val MEDIASTORE_ID = MediastoreSongEntity.Columns.ID
        const val INDEX = "index"
        const val PATH = "path"
    }

    companion object {
        const val TABLE_NAME = "playlist_songs"

        fun from(
            playlistEntity: PlaylistEntity,
            mediastoreSongEntity: MediastoreSongEntity,
            index: Int,
        ): PlaylistSongEntity = PlaylistSongEntity(
            mediastoreId = mediastoreSongEntity.mediastorId,
            playlistId = playlistEntity.id,
            index = index,
            path = mediastoreSongEntity.path
        )
    }
}