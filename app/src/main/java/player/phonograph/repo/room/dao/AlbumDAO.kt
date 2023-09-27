/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.SongMarker
import player.phonograph.repo.room.entity.Album
import player.phonograph.repo.room.entity.AlbumWithSongs
import androidx.room.*

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
