/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.Converters
import player.phonograph.repo.room.entity.Columns.DATE_MODIFIED
import player.phonograph.repo.room.entity.Columns.PATH
import player.phonograph.repo.room.entity.Columns.SONG_ID
import player.phonograph.repo.room.entity.Columns.TITLE
import player.phonograph.repo.room.entity.Song
import player.phonograph.repo.room.entity.Tables.SONGS
import androidx.room.*

@Dao
@TypeConverters(Converters::class)
interface SongDao {

    @Query("SELECT * from $SONGS order by :sortOrder")
    fun all(sortOrder: String = SONG_ID): List<Song>

    @Query("SELECT * from $SONGS where $SONG_ID = :id")
    fun id(id: Long): Song?
    @Query("SELECT * from $SONGS where $TITLE = :title")
    fun title(title: String): Song?
    @Query("SELECT * from $SONGS where $PATH like :path")
    fun path(path: String): Song?

    @Query("SELECT * from $SONGS where $DATE_MODIFIED > :time order by :sortOrder")
    fun since(time: Long, sortOrder: String = SONG_ID): List<Song>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun override(song: Song)

    @Update
    fun update(song: Song)

    @Delete
    fun delete(song: Song)
}
