/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.database.mediastore

import androidx.room.*

// @Fts3
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey // media store id
    var id: Long,
    @ColumnInfo
    var path: String,
    @ColumnInfo
    var title: String? = null,
    @ColumnInfo
    var year: Int = 0,
    @ColumnInfo
    var duration: Long = 0,
    @ColumnInfo(name = "date_modified")
    var dateModified: Long = 0,
    @ColumnInfo(name = "album_id")
    var albumId: Long = 0,
    @ColumnInfo(name = "album_name")
    var albumName: String? = null,
    @ColumnInfo(name = "artist_id")
    var artistId: Long = 0,
    @ColumnInfo(name = "artist_name")
    var artistName: String? = null,
    @ColumnInfo(name = "track_number")
    var trackNumber: Int = 0
)

@Dao
@TypeConverters(SongConverter::class)
interface SongDao {
    @Query("SELECT * from songs")
    fun getAllSongs(): List<Song>
    @Query("SELECT * from songs order by :sortOrder")
    fun getAllSongs(sortOrder: String): List<Song>

    @Query("SELECT * from songs where id = :id")
    fun findSongById(id: Long): List<Song>
    @Query("SELECT * from songs where title = :title")
    fun findSongByTitle(title: String): List<Song>
    @Query("SELECT * from songs where title like :title order by :sortOrder")
    fun searchSongByTitle(title: String, sortOrder: String): List<Song>
    @Query("SELECT * from songs where album_name = :album")
    fun findSongByAlbum(album: String): List<Song>
    @Query("SELECT * from songs where album_name like :album order by :sortOrder")
    fun searchSongByAlbum(album: String, sortOrder: String): List<Song>
    @Query("SELECT * from songs where artist_name = :artist")
    fun findSongByArtist(artist: String): List<Song>
    @Query("SELECT * from songs where artist_name like :artist order by :sortOrder")
    fun searchSongByArtist(artist: String, sortOrder: String): List<Song>

    @Query("SELECT * from songs where path in(:path) order by :sortOrder")
    fun querySongByPath(path: Array<String>, sortOrder: String): List<Song>

    @Query("SELECT * from songs where date_modified > :time order by :sortOrder")
    fun queryLastAddedSongs(time: Long, sortOrder: String): List<Song>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun override(song: Song)

    @Update
    fun update(song: Song)

    @Delete
    fun delete(song: Song)
}
