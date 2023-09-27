/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.SongMarker
import player.phonograph.repo.room.entity.Artist
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update

@Dao
@TypeConverters(SongMarker::class)
interface ArtistDAO {
    @Query("SELECT * from artists order by :sortOrder")
    fun getAllArtists(sortOrder: String): List<Artist>

    @Query("SELECT * from artists where artist_name like :artistName order by :sortOrder")
    fun searchArtists(artistName: String, sortOrder: String): List<Artist>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(artist: Artist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun override(artist: Artist)

    @Update
    fun update(artist: Artist)

    @Delete
    fun delete(artist: Artist)
}