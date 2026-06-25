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
import androidx.room.Query

@Dao
abstract class RelationshipQueryDao {

    // Album-Artist linkage queries

    @Query("SELECT * from $LINKAGE_ARTIST_ALBUM where ${Columns.ARTIST_ID} = :artistId")
    abstract suspend fun albumsOfArtist(artistId: Long): List<LinkageAlbumAndArtist>

    @Query("SELECT * from $LINKAGE_ARTIST_ALBUM where ${Columns.ALBUM_ID} = :albumId")
    abstract suspend fun artistsOfAlbum(albumId: Long): List<LinkageAlbumAndArtist>


    // Artist-Song linkage queries

    @Query("SELECT * from $LINKAGE_ARTIST_SONG where ${Columns.ARTIST_ID} = :artistId")
    abstract suspend fun songsOfArtist(artistId: Long): List<LinkageSongAndArtist>

    @Query("SELECT * from $LINKAGE_ARTIST_SONG where ${Columns.MEDIASTORE_ID} = :songId")
    abstract suspend fun artistsOfSong(songId: Long): List<LinkageSongAndArtist>

    @Query("SELECT * from $LINKAGE_ARTIST_SONG where ${Columns.MEDIASTORE_ID} in (:songIds)")
    abstract suspend fun artistsOfSongs(songIds: Collection<Long>): List<LinkageSongAndArtist>


    // Genre-Song linkage queries

    @Query("SELECT * from $LINKAGE_GENRE_SONG where ${Columns.GENRE_ID} = :genreId")
    abstract suspend fun songsOfGenre(genreId: Long): List<LinkageGenreAndSong>

    @Query("SELECT * from $LINKAGE_GENRE_SONG where ${Columns.MEDIASTORE_ID} = :songId")
    abstract suspend fun genresOfSong(songId: Long): List<LinkageGenreAndSong>

    @Query("SELECT * from $LINKAGE_GENRE_SONG where ${Columns.MEDIASTORE_ID} in (:songIds)")
    abstract suspend fun genresOfSongs(songIds: Collection<Long>): List<LinkageGenreAndSong>

    @Query("SELECT ${Columns.MEDIASTORE_ID} from $LINKAGE_GENRE_SONG where ${Columns.GENRE_ID} = :genreId")
    abstract suspend fun songIdsOfGenre(genreId: Long): List<Long>
}
