/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Tables.ARTISTS
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ArtistDao {

    @Query("SELECT * from $ARTISTS order by :sortOrder")
    fun all(sortOrder: String = ARTIST_ID): List<ArtistEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(artist: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun override(artist: ArtistEntity)

    @Update
    fun update(artist: ArtistEntity)

    @Delete
    fun delete(artist: ArtistEntity)
}