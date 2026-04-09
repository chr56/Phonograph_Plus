/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.LinkageGenreAndSong
import player.phonograph.repo.room.entity.Tables.LINKAGE_GENRE_SONG
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class RelationshipGenreSongDao {

    @Query("SELECT * from $LINKAGE_GENRE_SONG where ${Columns.GENRE_ID} = :genreId")
    abstract fun genre(genreId: Long): List<LinkageGenreAndSong> // get by genre id

    @Query("SELECT * from $LINKAGE_GENRE_SONG where ${Columns.MEDIASTORE_ID} = :songId")
    abstract fun song(songId: Long): List<LinkageGenreAndSong> // get by song id

    @Query("SELECT * from $LINKAGE_GENRE_SONG where ${Columns.MEDIASTORE_ID} in (:songIds)")
    abstract fun songs(songIds: Collection<Long>): List<LinkageGenreAndSong> // get by song ids

    @Query("SELECT ${Columns.MEDIASTORE_ID} from $LINKAGE_GENRE_SONG where ${Columns.GENRE_ID} = :genreId")
    abstract fun songIds(genreId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun override(linkage: LinkageGenreAndSong)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun override(linkages: List<LinkageGenreAndSong>)

    @Delete
    abstract fun remove(linkage: LinkageGenreAndSong): Int

    @Delete
    abstract fun remove(linkages: List<LinkageGenreAndSong>): Int

    @Query("DELETE FROM $LINKAGE_GENRE_SONG where ${Columns.GENRE_ID} = :genreId")
    abstract fun removeGenre(genreId: Long): Int

    @Query("DELETE FROM $LINKAGE_GENRE_SONG where ${Columns.MEDIASTORE_ID} = :songId")
    abstract fun removeSong(songId: Long): Int

    @Query("DELETE FROM $LINKAGE_GENRE_SONG where ${Columns.GENRE_ID} in (:genreIds)")
    abstract fun removeGenres(genreIds: Collection<Long>): Int

    @Query("DELETE FROM $LINKAGE_GENRE_SONG where ${Columns.MEDIASTORE_ID} in (:songIds)")
    abstract fun removeSongs(songIds: Collection<Long>): Int

    @Query("DELETE FROM $LINKAGE_GENRE_SONG")
    abstract suspend fun deleteAll(): Int
}
