/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.database.mediastore

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

object AlbumColumns {
    const val ALBUM_ID = "album_id"
    const val ALBUM_NAME = "album_name"
    const val ALBUM_ARTIST = "album_artist"
    const val YEAR = "year"
    const val SONG_COUNT = "song_count"
}

@Entity(tableName = "albums", primaryKeys = ["album_id"])
data class Album(
    @ColumnInfo(name = AlbumColumns.ALBUM_ID)
    var albumId: Long = 0,
    @ColumnInfo(name = AlbumColumns.ALBUM_NAME)
    var albumName: String? = null,
    @ColumnInfo(name = AlbumColumns.ALBUM_ARTIST)
    var albumArtist: String,
    @ColumnInfo(name = AlbumColumns.YEAR)
    var year: Int,
    @ColumnInfo(name = AlbumColumns.SONG_COUNT)
    var songCount: Int,
)

data class AlbumWithSongs(
    @Embedded var album: Album,
    @Relation(
        parentColumn = "album_id",
        entityColumn = "album_id"
    )
    var songs: List<Song>
)

@Dao
interface AlbumDAO {
    @RawQuery
    fun rawQuery(query: SupportSQLiteQuery): List<Album>

    fun getAllAlbums(columns: String, order: Boolean = true): List<Album> {
        // todo valid input
        val query =
            if (columns.isNotBlank()) "SELECT * from albums order by ${if (order) "$columns ASC" else "$columns DESC"}"
            else "SELECT * FROM albums"

        return rawQuery(SimpleSQLiteQuery(query))
    }

    @Query("SELECT * from albums where album_name like :albumName")
    fun searchAlbums(albumName: String): List<Album>

    @Query("SELECT * from albums where album_id = :albumId")
    fun findAlbum(albumId: Long): Album

    @Transaction
    @Query("SELECT * from albums where album_id = :albumId or album_name like :albumName")
    fun getAlbumsWithSongs(albumId: Long = 0, albumName: String = ""): AlbumWithSongs

    @Transaction
    @Query("SELECT * from albums")
    fun getAllAlbumsWithSongs(): List<AlbumWithSongs>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun override(album: Album)

    @Update
    fun update(album: Album)

    @Delete
    fun delete(album: Album)
}
