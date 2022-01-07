/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.database.mediastore

import androidx.room.*

@Entity(tableName = "artists", primaryKeys = ["artist_id"])
data class Artist(
    @ColumnInfo(name = "artist_id")
    var artistId: Long = 0,
    @ColumnInfo(name = "artist_name")
    var artistName: String? = null
)

@Dao
interface ArtistDAO {
    @Query("SELECT * from artists order by :sortOrder")
    fun getAllArtists(sortOrder: String): List<Artist>

    @Query("SELECT * from artists where artist_name like :artistName order by :sortOrder")
    fun searchArtists(artistName: String, sortOrder: String): List<Artist>
}
