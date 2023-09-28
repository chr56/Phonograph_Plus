/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.ROLE
import player.phonograph.repo.room.entity.Columns.SONG_ID
import androidx.annotation.IntDef
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = Tables.ARTIST_SONG_LINKAGE,
    primaryKeys = [ARTIST_ID, SONG_ID],
    indices = [Index(value = [SONG_ID, ARTIST_ID])]
)
data class SongAndArtistLinkage(
    @ColumnInfo(name = SONG_ID)
    var songId: Long,
    @ColumnInfo(name = ARTIST_ID)
    var artistId: Long,
    @ColumnInfo(name = ROLE, defaultValue = "$ROLE_ARTIST")
    @ArtistRole
    var role: Int,
) {

    companion object {
        const val ROLE_ARTIST = 0
        const val ROLE_COMPOSER = 6
        const val ROLE_ALBUM_ARTIST = 10
    }

    @IntDef(ROLE_ARTIST, ROLE_COMPOSER, ROLE_ALBUM_ARTIST)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ArtistRole
}