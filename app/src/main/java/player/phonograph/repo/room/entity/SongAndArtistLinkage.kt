/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.entity

import androidx.annotation.IntDef
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "artist_song_linkage",
    primaryKeys = ["artist_id", "song_id"],
    indices = [Index(value = ["song_id", "artist_id"])]
)
data class SongAndArtistLinkage(
    @ColumnInfo(name = "song_id")
    var songId: Long,
    @ColumnInfo(name = "artist_id")
    var artistId: Long,
    @ColumnInfo(name = "role", defaultValue = "$ROLE_ARTIST")
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