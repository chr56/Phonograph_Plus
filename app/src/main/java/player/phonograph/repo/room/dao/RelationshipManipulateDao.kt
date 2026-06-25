/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.LinkageAlbumAndArtist
import player.phonograph.repo.room.entity.LinkageGenreAndSong
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.Tables.LINKAGE_ARTIST_ALBUM
import player.phonograph.repo.room.entity.Tables.LINKAGE_ARTIST_SONG
import player.phonograph.repo.room.entity.Tables.LINKAGE_GENRE_SONG
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class RelationshipManipulateDao {

    // Album-Artist linkage writes

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun overrideAlbumArtist(linkage: LinkageAlbumAndArtist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun overrideAlbumArtists(linkages: List<LinkageAlbumAndArtist>)

    @Delete
    abstract suspend fun removeAlbumArtist(linkage: LinkageAlbumAndArtist): Int

    @Query("DELETE FROM $LINKAGE_ARTIST_ALBUM")
    abstract suspend fun deleteAllAlbumArtists(): Int

    @Query("DELETE FROM $LINKAGE_ARTIST_ALBUM where ${Columns.ALBUM_ID} = :albumId")
    abstract suspend fun removeAlbum(albumId: Long): Int

    @Query("DELETE FROM $LINKAGE_ARTIST_ALBUM where ${Columns.ARTIST_ID} = :artistId")
    abstract suspend fun removeArtist(artistId: Long): Int

    @Query("DELETE FROM $LINKAGE_ARTIST_ALBUM where ${Columns.ALBUM_ID} in (:albumIds)")
    abstract suspend fun removeAlbums(albumIds: Collection<Long>): Int

    @Query("DELETE FROM $LINKAGE_ARTIST_ALBUM where ${Columns.ARTIST_ID} in (:artistIds)")
    abstract suspend fun removeArtists(artistIds: Collection<Long>): Int


    // Artist-Song linkage writes

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun overrideArtistSong(linkage: LinkageSongAndArtist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun overrideArtistSongs(linkages: List<LinkageSongAndArtist>)

    @Delete
    abstract suspend fun removeArtistSong(linkage: LinkageSongAndArtist): Int

    @Delete
    abstract suspend fun removeArtistSongs(linkages: List<LinkageSongAndArtist>): Int

    @Query("DELETE FROM $LINKAGE_ARTIST_SONG")
    abstract suspend fun deleteAllArtistSongs(): Int


    // Genre-Song linkage writes

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun overrideGenreSong(linkage: LinkageGenreAndSong)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun overrideGenreSongs(linkages: List<LinkageGenreAndSong>)

    @Delete
    abstract suspend fun removeGenreSong(linkage: LinkageGenreAndSong): Int

    @Delete
    abstract suspend fun removeGenreSongs(linkages: List<LinkageGenreAndSong>): Int

    @Query("DELETE FROM $LINKAGE_GENRE_SONG where ${Columns.GENRE_ID} = :genreId")
    abstract suspend fun removeGenre(genreId: Long): Int

    @Query("DELETE FROM $LINKAGE_GENRE_SONG where ${Columns.MEDIASTORE_ID} = :songId")
    abstract suspend fun removeSong(songId: Long): Int

    @Query("DELETE FROM $LINKAGE_GENRE_SONG where ${Columns.GENRE_ID} in (:genreIds)")
    abstract suspend fun removeGenres(genreIds: Collection<Long>): Int

    @Query("DELETE FROM $LINKAGE_GENRE_SONG where ${Columns.MEDIASTORE_ID} in (:songIds)")
    abstract suspend fun removeSongs(songIds: Collection<Long>): Int

    @Query("DELETE FROM $LINKAGE_GENRE_SONG")
    abstract suspend fun deleteAllGenreSongs(): Int
}
