/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.database.mediastore

import androidx.room.*
import player.phonograph.App
import player.phonograph.util.PreferenceUtil

// @Fts3
@Entity(tableName = "songs")
@TypeConverters(SongConverter::class)
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
interface SongDao {
    @Query("SELECT * from songs")
    fun getAllSongs(): List<Song>

    @Query("SELECT * from songs where id = :id")
    fun findSongById(id: Long): List<Song>
    @Query("SELECT * from songs where title = :title")
    fun findSongByTitle(title: String): List<Song>
    @Query("SELECT * from songs where title like :title")
    fun searchSongByTitle(title: String): List<Song>
    @Query("SELECT * from songs where album_name = :album")
    fun findSongByAlbum(album: String): List<Song>
    @Query("SELECT * from songs where artist_name = :artist")
    fun findSongByArtist(artist: String): List<Song>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun override(song: Song)

    @Update
    fun update(song: Song)

    @Delete
    fun delete(song: Song)
}

@Database(entities = arrayOf(Song::class), version = 1, exportSchema = false)
abstract class SongDataBase : RoomDatabase() {
    abstract fun SongDao(): SongDao
    var lastUpdateTimestamp: Long = 0
        get() = PreferenceUtil.getInstance(App.instance).lastMusicDatabaseUpdateTimestamp
        set(value) { field = value; PreferenceUtil.getInstance(App.instance).lastMusicDatabaseUpdateTimestamp = value }
    var lastAccessTimestamp: Long = 0
        get() = PreferenceUtil.getInstance(App.instance).lastMusicDatabaseAccessTimestamp
        set(value) { field = value; PreferenceUtil.getInstance(App.instance).lastMusicDatabaseAccessTimestamp = value }
}

object MusicDatabase {
    private fun initSongDataBase(): SongDataBase {
        // todo disable allowMainThreadQueries
        return Room.databaseBuilder(App.instance, SongDataBase::class.java, "musicV1.db")
            .allowMainThreadQueries().build()
    }

    var songsDataBase: SongDataBase? = null
        get() = if (field == null) {
            initSongDataBase()
        } else field
        private set
}

// todo remove
object SongConverter {
    @TypeConverter
    fun fromSongModel(song: player.phonograph.model.Song): Song {
        return Song(
            song.id, song.data, song.title, song.year, song.duration, song.dateModified, song.albumId, song.albumName, song.artistId, song.artistName, song.trackNumber
        )
    }

    @TypeConverter
    fun toSongModel(song: Song): player.phonograph.model.Song {
        return player.phonograph.model.Song(
            song.id, song.title, song.trackNumber, song.year, song.duration, song.path, song.dateModified, song.albumId, song.albumName, song.artistId, song.artistName
        )
    }
}
