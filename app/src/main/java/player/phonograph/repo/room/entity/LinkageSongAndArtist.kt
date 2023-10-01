/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.annotation.IntDef
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = Tables.LINKAGE_ARTIST_SONG,
    primaryKeys = [Columns.ARTIST_ID, Columns.MEDIASTORE_ID],
    indices = [Index(value = [Columns.MEDIASTORE_ID, Columns.ARTIST_ID])]
)
data class LinkageSongAndArtist(
    @ColumnInfo(name = Columns.MEDIASTORE_ID)
    var songId: Long,
    @ColumnInfo(name = Columns.ARTIST_ID)
    var artistId: Long,
    @ColumnInfo(name = Columns.ROLE, defaultValue = "$ROLE_ARTIST")
    @param:ArtistRole @get:ArtistRole @property:ArtistRole
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