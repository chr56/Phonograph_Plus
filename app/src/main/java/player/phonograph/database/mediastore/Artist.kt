/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.database.mediastore

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

object ArtistColumns {
    const val ARTIST_ID = "artist_id"
    const val ARTIST_NAME = "artist_name"
    const val SONG_COUNT = "song_count"
    const val ALBUM_COUNT = "album_count"
}

@Entity(tableName = "artists", primaryKeys = ["artist_id", "artist_name"])
data class Artist(
    @ColumnInfo(name = ArtistColumns.ARTIST_ID)
    var artistId: Long = 0,
    @ColumnInfo(name = ArtistColumns.ARTIST_NAME)
    var artistName: String,
    @ColumnInfo(name = ArtistColumns.SONG_COUNT)
    var songCount: Int,
    @ColumnInfo(name = ArtistColumns.ALBUM_COUNT)
    var albumCount: Int,

)

@Entity(tableName = "artist_song_linkage", primaryKeys = ["artist_id", "id"])
data class SongAndArtistLinkage(
    @ColumnInfo(name = "id")
    var songId: Long,
    @ColumnInfo(name = "artist_id")
    var artistId: Long,
)
@Entity(tableName = "artist_album_linkage", primaryKeys = ["artist_id", "album_id"])
data class ArtistAndAlbumLinkage(
    @ColumnInfo(name = "album_id", index = true)
    var albumId: Long,
    @ColumnInfo(name = "artist_id", index = true)
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

data class ArtistWithAlbums(
    @Embedded var artist: Artist,
    @Relation(
        parentColumn = "artist_id",
        entityColumn = "album_id",
        associateBy = Junction(ArtistAndAlbumLinkage::class)
    )
    var albums: List<Album>
)

@Dao
interface ArtistDAO {

    @RawQuery
    fun rawQuery(query: SupportSQLiteQuery): List<Artist>

    fun getAllArtists(columns: String, order: Boolean = true): List<Artist> {
        // todo valid input
        val query =
            if (columns.isNotBlank()) "SELECT * from artists order by ${if (order) "$columns ASC" else "$columns DESC"}"
            else "SELECT * FROM artists"

        return rawQuery(SimpleSQLiteQuery(query))
    }

    @Query("SELECT * from artists where artist_name like :artistName")
    fun searchArtists(artistName: String): List<Artist>

    @Query("SELECT * from artists where artist_id = :artistId")
    fun findArtist(artistId: Long): Artist

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
    @Query("SELECT * from artists where artist_name like :artistName ")
    fun getArtistSong(artistName: String): ArtistWithSongs

    @Transaction
    @Query("SELECT * from artists")
    fun getAllArtistSong(): List<ArtistWithSongs>

    @Transaction
    @Query("SELECT * from songs where id like :songId")
    fun getArtistBySong(songId: Long): SongWithArtists

// useful?
//    @Transaction
//    @Query("SELECT * from songs")
//    fun getArtistByAllSong(): List<SongWithArtists>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun override(linkage: SongAndArtistLinkage)

    @Delete
    fun remove(linkage: SongAndArtistLinkage)
}

@Dao
interface ArtistAlbumDAO{

    @Transaction
    @Query("SELECT * from artists where artist_name like :artistName ")
    fun getArtistAlbum(artistName: String): ArtistWithAlbums

    @Transaction
    @Query("SELECT * from artists")
    fun getAllArtistAlbum(): List<ArtistWithAlbums>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun override(linkage: ArtistAndAlbumLinkage)

    @Delete
    fun remove(linkage: ArtistAndAlbumLinkage)
}
