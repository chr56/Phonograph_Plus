/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.ArtistWithSongs
import player.phonograph.repo.room.entity.SongAndArtistLinkage
import player.phonograph.repo.room.entity.SongWithArtists
import androidx.room.*

@Dao
interface ArtistSongDAO {

    @Transaction
    @Query("SELECT * from artists where artist_name like :artistName order by :sortOrder")
    fun getArtistSong(artistName: String, sortOrder: String): ArtistWithSongs

    @Transaction
    @Query("SELECT * from artists order by :sortOrder")
    fun getAllArtistSong(sortOrder: String): List<ArtistWithSongs>

    @Transaction
    @Query("SELECT * from songs where song_id like :songId order by :sortOrder")
    fun getArtistBySong(songId: Long, sortOrder: String): SongWithArtists

    @Transaction
    @Query("SELECT * from songs order by :sortOrder")
    fun getArtistByAllSong(sortOrder: String): List<SongWithArtists>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun override(linkage: SongAndArtistLinkage)

    @Delete
    fun remove(linkage: SongAndArtistLinkage)
}
