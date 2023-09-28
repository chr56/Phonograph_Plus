/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.AlbumWithSongs
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.ArtistWithSongs
import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Columns.ALBUM_NAME
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.ARTIST_NAME
import player.phonograph.repo.room.entity.Columns.RAW_ARTIST_NAME
import player.phonograph.repo.room.entity.Columns.SONG_ID
import player.phonograph.repo.room.entity.Columns.TITLE
import player.phonograph.repo.room.entity.SongEntity
import player.phonograph.repo.room.entity.SongWithArtists
import player.phonograph.repo.room.entity.Tables.ALBUMS
import player.phonograph.repo.room.entity.Tables.ARTISTS
import player.phonograph.repo.room.entity.Tables.SONGS
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface QueryDao {

    // Search Songs
    @Query("SELECT * from $SONGS where $TITLE like :title order by :sortOrder")
    fun songsWithTitle(title: String, sortOrder: String): List<SongEntity>
    @Query("SELECT * from $SONGS where $ALBUM_NAME like :albumName order by :sortOrder")
    fun songsWithAlbum(albumName: String, sortOrder: String): List<SongEntity>
    @Query("SELECT * from $SONGS where $RAW_ARTIST_NAME like :artistName order by :sortOrder")
    fun songsWithArtist(artistName: String, sortOrder: String): List<SongEntity>

    // Search Albums
    @Query("SELECT * from $ALBUMS where $ALBUM_NAME like :albumName order by :sortOrder")
    fun albumsWithName(albumName: String, sortOrder: String = ALBUM_ID): List<AlbumEntity>

    // Search Artist
    @Query("SELECT * from $ARTISTS where $ARTIST_NAME like :artistName order by :sortOrder")
    fun artistsWithName(artistName: String, sortOrder: String = ARTIST_ID): List<ArtistEntity>


    // Relationship


    @Transaction
    @Query("SELECT * from $ARTISTS where $ARTIST_ID = :artistId order by :sortOrder")
    fun artistSongs(artistId: Long, sortOrder: String): ArtistWithSongs

    @Transaction
    @Query("SELECT * from $ALBUMS where $ALBUM_ID = :albumId order by :sortOrder")
    fun albumSongs(albumId: Long, sortOrder: String): AlbumWithSongs

    @Transaction
    @Query("SELECT * from $SONGS where $SONG_ID = :songId order by :sortOrder")
    fun artistsOfSong(songId: Long, sortOrder: String = ARTIST_ID): SongWithArtists


    @Transaction
    @Query("SELECT * from $SONGS order by :sortOrder")
    fun artistsOfAllSongs(sortOrder: String = ARTIST_ID): List<SongWithArtists>

    @Transaction
    @Query("SELECT * from $ARTISTS order by :sortOrder")
    fun allArtistSongs(sortOrder: String): List<ArtistWithSongs>

}