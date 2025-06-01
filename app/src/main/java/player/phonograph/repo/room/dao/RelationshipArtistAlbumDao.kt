/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.LinkageAlbumAndArtist
import player.phonograph.repo.room.entity.Tables.LINKAGE_ARTIST_ALBUM
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class RelationshipArtistAlbumDao {

    @Query("SELECT * from $LINKAGE_ARTIST_ALBUM where ${Columns.ARTIST_ID} = :artistId")
    abstract fun artist(artistId: Long): List<LinkageAlbumAndArtist>

    @Query("SELECT * from $LINKAGE_ARTIST_ALBUM where ${Columns.ALBUM_ID} = :albumId")
    abstract fun album(albumId: Long): List<LinkageAlbumAndArtist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun override(linkage: LinkageAlbumAndArtist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun override(linkages: List<LinkageAlbumAndArtist>)

    @Delete
    abstract fun remove(linkage: LinkageAlbumAndArtist): Int

    fun removeAlbum(albumId: Long) {
        for (item in album(albumId)) {
            remove(item)
        }
    }

    fun removeArtist(artistId: Long) {
        for (item in artist(artistId)) {
            remove(item)
        }
    }
}