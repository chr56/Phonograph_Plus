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
    var albumName: String? = null,

)

@Dao
interface AlbumDAO {
    @Query("SELECT * from albums order by :sortOrder")
    fun getAllAlbums(sortOrder: String): List<Album>

    @Query("SELECT * from albums where album_name like :albumName order by :sortOrder")
    fun searchAlbums(albumName: String, sortOrder: String): List<Album>
}
