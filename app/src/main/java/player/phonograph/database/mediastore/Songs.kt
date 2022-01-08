/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.database.mediastore

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

/**
 * available sort orders
 */
object SongColumns {
    const val ID = "id"
    const val PATH = "path"
    const val SIZE = "size"
    const val DISPLAY_NAME = "display_name"
    const val DATE_ADDED = "date_added"
    const val DATE_MODIFIED = "date_modified"
    const val TITLE = "title"
    const val ALBUM_ID = "album_id"
    const val ALBUM_NAME = "album_name"
    const val ARTIST_ID = "artist_id"
    const val ARTIST_NAME = "artist_name"
    const val YEAR = "year"
    const val DURATION = "duration"
    const val TRACK_NUMBER = "track_number"
}

// @Fts3
@Entity(
    tableName = "songs",
    indices = [Index(value = ["id"])]
)
data class Song(
    @PrimaryKey
    @ColumnInfo(name = SongColumns.ID)
    var id: Long, // media store id
    @ColumnInfo(name = SongColumns.PATH)
    var path: String,
    @ColumnInfo(name = SongColumns.SIZE)
    var size: Long,

    @ColumnInfo(name = SongColumns.DISPLAY_NAME)
    var displayName: String? = null,
    @ColumnInfo(name = SongColumns.DATE_ADDED)
    var dateAdded: Long = 0,
    @ColumnInfo(name = SongColumns.DATE_MODIFIED)
    var dateModified: Long = 0,

    var title: String? = null,
    @ColumnInfo(name = SongColumns.ALBUM_ID)
    var albumId: Long = 0,
    @ColumnInfo(name = SongColumns.ALBUM_NAME)
    var albumName: String? = null,
    @ColumnInfo(name = SongColumns.ARTIST_ID)
    var artistId: Long = 0,
    @ColumnInfo(name = SongColumns.ARTIST_NAME)
    var artistName: String? = null,

    @ColumnInfo(name = SongColumns.YEAR)
    var year: Int = 0,
    @ColumnInfo(name = SongColumns.DURATION)
    var duration: Long = 0,
    @ColumnInfo(name = SongColumns.TRACK_NUMBER)
    var trackNumber: Int = 0
)

@Dao
@TypeConverters(SongConverter::class)
interface SongDao {
    @Query("SELECT * from songs")
    fun getAllSongs(): List<Song>

    @RawQuery
    fun rawQuery(query: SupportSQLiteQuery): List<Song>

    /**
     * @param columns The columns must be one string in [SongColumns]
     * @param order true - Asc; false- Desc
     */
    fun getAllSongs(columns: String, order: Boolean = true): List<Song> {
        // todo valid input
        val query = "SELECT * FROM songs ORDER BY ${if (order) "$columns ASC" else "$columns DESC"}"

        return rawQuery(SimpleSQLiteQuery(query))
    }

    @Query("SELECT * from songs where id = :id")
    fun findSongById(id: Long): Song
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
