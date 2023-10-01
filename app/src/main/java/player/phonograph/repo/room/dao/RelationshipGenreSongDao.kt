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

    fun removeGenre(genreId: Long) {
        for (item in genre(genreId)) {
            remove(item)
        }
    }

    fun removeSong(songId: Long) {
        for (item in song(songId)) {
            remove(item)
        }
    }

    @Query("DELETE FROM $LINKAGE_GENRE_SONG")
    abstract suspend fun deleteAll(): Int
}