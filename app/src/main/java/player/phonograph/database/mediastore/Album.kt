/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.database.mediastore

import androidx.room.*

@Entity(tableName = "albums", primaryKeys = ["album_id"])
data class Album(
    @ColumnInfo(name = "album_id")
    var albumId: Long = 0,
    @ColumnInfo(name = "album_name")
    var albumName: String? = null
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
@TypeConverters(SongMarker::class)
interface AlbumDAO {
    @Query("SELECT * from albums order by :sortOrder")
    fun getAllAlbums(sortOrder: String): List<Album>

    @Query("SELECT * from albums where album_name like :albumName order by :sortOrder")
    fun searchAlbums(albumName: String, sortOrder: String): List<Album>

    @Transaction
    @Query("SELECT * from albums where album_id = :albumId or album_name like :albumName order by :sortOrder")
    fun getAlbumsWithSongs(albumId: Long = 0, albumName: String = "", sortOrder: String): List<AlbumWithSongs>

    @Transaction
    @Query("SELECT * from albums order by :sortOrder")
    fun getAllAlbumsWithSongs(sortOrder: String): List<AlbumWithSongs>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(album: Album)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun override(album: Album)

    @Update
    fun update(album: Album)

    @Delete
    fun delete(album: Album)
}
