/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.App
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import android.content.SharedPreferences

object MusicDatabase {
    private var innerSongsDataBase: SongDataBase? = null

    private fun initSongDataBase() {
        // todo disable allowMainThreadQueries
        innerSongsDataBase = Room.databaseBuilder(
            App.instance,
            SongDataBase::class.java,
            "musicV1.db"
        ).allowMainThreadQueries().build()
    }

    val songsDataBase: SongDataBase
        get() {
            if (innerSongsDataBase == null) initSongDataBase()
            return innerSongsDataBase!!
        }


    object Metadata {
        private const val PREFERENCE_NAME = "database_meta"
        private val sharedPreferences: SharedPreferences =
            App.instance.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

        var lastUpdateTimestamp: Long
            get() = sharedPreferences.getLong(LAST_MUSIC_DATABASE_UPDATE_TIMESTAMP, -1L)
            set(value) {
                sharedPreferences.edit().putLong(LAST_MUSIC_DATABASE_UPDATE_TIMESTAMP, value).apply()
            }
        var lastAccessTimestamp: Long
            get() = sharedPreferences.getLong(LAST_MUSIC_DATABASE_ACCESS_TIMESTAMP, -1L)
            set(value) {
                sharedPreferences.edit().putLong(LAST_MUSIC_DATABASE_ACCESS_TIMESTAMP, value).apply()
            }

        private const val LAST_MUSIC_DATABASE_UPDATE_TIMESTAMP = "last_music_database_update_timestamp"
        private const val LAST_MUSIC_DATABASE_ACCESS_TIMESTAMP = "last_music_database_access_timestamp"
    }
}

@Database(
    entities = [Song::class, Album::class, Artist::class, SongAndArtistLinkage::class],
    version = 1,
    exportSchema = true,
)
abstract class SongDataBase : RoomDatabase() {
    abstract fun SongDao(): SongDao
    abstract fun AlbumDao(): AlbumDAO
    abstract fun ArtistDao(): ArtistDAO
    abstract fun ArtistSongsDao(): ArtistSongDAO
}
