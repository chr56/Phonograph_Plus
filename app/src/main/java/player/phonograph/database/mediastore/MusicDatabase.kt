/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.database.mediastore

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import player.phonograph.App
import player.phonograph.util.PreferenceUtil

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

@Database(entities = [Song::class,Album::class], version = 1, exportSchema = false)
abstract class SongDataBase : RoomDatabase() {
    abstract fun SongDao(): SongDao
    abstract fun AlbumDao(): AlbumDAO
    var lastUpdateTimestamp: Long = 0
        get() = PreferenceUtil.getInstance(App.instance).lastMusicDatabaseUpdateTimestamp
        set(value) { field = value; PreferenceUtil.getInstance(App.instance).lastMusicDatabaseUpdateTimestamp = value }
    var lastAccessTimestamp: Long = 0
        get() = PreferenceUtil.getInstance(App.instance).lastMusicDatabaseAccessTimestamp
        set(value) { field = value; PreferenceUtil.getInstance(App.instance).lastMusicDatabaseAccessTimestamp = value }
}
