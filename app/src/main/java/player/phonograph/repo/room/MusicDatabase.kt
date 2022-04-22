/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import player.phonograph.App

object MusicDatabase {
    private var innerSongsDataBase: SongDataBase? = null

    private fun initSongDataBase() {
        // todo disable allowMainThreadQueries
        innerSongsDataBase = Room.databaseBuilder(
            App.instance,
            SongDataBase::class.java,
            "musicV1.db"
        )
            .allowMainThreadQueries().build()
    }

    val songsDataBase: SongDataBase
        get() {
            if (innerSongsDataBase == null) { initSongDataBase() }
            return innerSongsDataBase!!
        }
}

@Database(entities = [Song::class, Album::class, Artist::class, SongAndArtistLinkage::class], version = 1, exportSchema = false)
abstract class SongDataBase : RoomDatabase() {
    abstract fun SongDao(): SongDao
    abstract fun AlbumDao(): AlbumDAO
    abstract fun ArtistDao(): ArtistDAO
    abstract fun ArtistSongsDao(): ArtistSongDAO
//    var lastUpdateTimestamp: Long = -1L
//        get() = Setting.instance.lastMusicDatabaseUpdateTimestamp
//        set(value) { field = value; Setting.instance.lastMusicDatabaseUpdateTimestamp = value }
//    var lastAccessTimestamp: Long = -1L
//        get() = Setting.instance.lastMusicDatabaseAccessTimestamp
//        set(value) { field = value; Setting.instance.lastMusicDatabaseAccessTimestamp = value }
}
