/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.database.mediastore

import androidx.room.*

@Entity(tableName = "artists", primaryKeys = ["artist_id", "artist_name"])
data class Artist(
    @ColumnInfo(name = "artist_id")
    var artistId: Long = 0,
    @ColumnInfo(name = "artist_name")
    var artistName: String
)

@Entity(tableName = "artist_song_linkage", primaryKeys = ["artist_id", "id"])
data class SongAndArtistLinkage(
    @ColumnInfo(name = "id")
    var songId: Long,
    @ColumnInfo(name = "artist_id")
    var artistId: Long,
)

data class ArtistWithSongs(
    @Embedded var artist: Artist,
    @Relation(
        parentColumn = "artist_id",
        entityColumn = "id",
        associateBy = Junction(SongAndArtistLinkage::class)
    )
    var songs: List<Song>
)
data class SongWithArtists(
    @Embedded var song: Song,
    @Relation(
        parentColumn = "id",
        entityColumn = "artist_id",
        associateBy = Junction(SongAndArtistLinkage::class)
    )
    var artist: List<Artist>
)

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

@Dao
interface ArtistSongDAO {

    @Transaction
    @Query("SELECT * from artists where artist_name like :artistName order by :sortOrder")
    fun getArtistSong(artistName: String, sortOrder: String): ArtistWithSongs

    @Transaction
    @Query("SELECT * from artists order by :sortOrder")
    fun getAllArtistSong(sortOrder: String): List<ArtistWithSongs>

    @Transaction
    @Query("SELECT * from songs where id like :songId order by :sortOrder")
    fun getArtistBySong(songId: Long, sortOrder: String): SongWithArtists

    @Transaction
    @Query("SELECT * from songs order by :sortOrder")
    fun getArtistByAllSong(sortOrder: String): List<SongWithArtists>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun override(linkage: SongAndArtistLinkage)

    @Delete
    fun remove(linkage: SongAndArtistLinkage)
}
