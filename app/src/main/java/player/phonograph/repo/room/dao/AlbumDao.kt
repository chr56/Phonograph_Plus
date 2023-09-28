/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Album
import player.phonograph.repo.room.entity.AlbumWithSongs
import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Columns.ALBUM_NAME
import player.phonograph.repo.room.entity.Tables.ALBUMS
import androidx.room.*

@Dao
interface AlbumDao {

    @Query("SELECT * from $ALBUMS order by :sortOrder")
    fun all(sortOrder: String): List<Album>

    @Transaction
    @Query("SELECT * from $ALBUMS where $ALBUM_ID = :albumId or $ALBUM_NAME like :albumName order by :sortOrder")
    fun getAlbumsWithSongs(albumId: Long, albumName: String, sortOrder: String): List<AlbumWithSongs>

    @Transaction
    @Query("SELECT * from $ALBUMS order by :sortOrder")
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
