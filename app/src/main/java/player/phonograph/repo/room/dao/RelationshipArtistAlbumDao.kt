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

    @Query("DELETE FROM $LINKAGE_ARTIST_ALBUM")
    abstract suspend fun deleteAll(): Int

    @Query("DELETE FROM $LINKAGE_ARTIST_ALBUM where ${Columns.ALBUM_ID} = :albumId")
    abstract fun removeAlbum(albumId: Long): Int

    @Query("DELETE FROM $LINKAGE_ARTIST_ALBUM where ${Columns.ARTIST_ID} = :artistId")
    abstract fun removeArtist(artistId: Long): Int

    @Query("DELETE FROM $LINKAGE_ARTIST_ALBUM where ${Columns.ALBUM_ID} in (:albumIds)")
    abstract fun removeAlbums(albumIds: Collection<Long>): Int

    @Query("DELETE FROM $LINKAGE_ARTIST_ALBUM where ${Columns.ARTIST_ID} in (:artistIds)")
    abstract fun removeArtists(artistIds: Collection<Long>): Int
}