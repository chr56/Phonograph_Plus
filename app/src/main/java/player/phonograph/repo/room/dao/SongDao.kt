/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.Converters
import player.phonograph.repo.room.entity.Columns.DATE_MODIFIED
import player.phonograph.repo.room.entity.Columns.PATH
import player.phonograph.repo.room.entity.Columns.SONG_ID
import player.phonograph.repo.room.entity.Columns.TITLE
import player.phonograph.repo.room.entity.SongEntity
import player.phonograph.repo.room.entity.Tables.SONGS
import androidx.room.*

@Dao
@TypeConverters(Converters::class)
interface SongDao {

    @Query("SELECT * from $SONGS order by :sortOrder")
    fun all(sortOrder: String = SONG_ID): List<SongEntity>

    @Query("SELECT * from $SONGS where $SONG_ID = :id")
    fun id(id: Long): SongEntity?
    @Query("SELECT * from $SONGS where $TITLE = :title")
    fun title(title: String): SongEntity?
    @Query("SELECT * from $SONGS where $PATH like :path")
    fun path(path: String): SongEntity?

    @Query("SELECT * from $SONGS where $DATE_MODIFIED > :time order by :sortOrder")
    fun since(time: Long, sortOrder: String = SONG_ID): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(songEntity: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun override(songEntity: SongEntity)

    @Update
    fun update(songEntity: SongEntity)

    @Delete
    fun delete(songEntity: SongEntity)
}
