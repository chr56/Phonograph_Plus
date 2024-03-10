/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.App
import player.phonograph.repo.room.dao.MediaStoreSongDao
import player.phonograph.repo.room.dao.PlaylistDao
import player.phonograph.repo.room.dao.PlaylistSongDao
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.PlaylistEntity
import player.phonograph.repo.room.entity.PlaylistSongEntity
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context


@Database(
    entities = [MediastoreSongEntity::class, PlaylistEntity::class, PlaylistSongEntity::class],
    version = MusicDatabase.DATABASE_VERSION,
    exportSchema = true,
)
abstract class SongDatabase : RoomDatabase() {
    abstract fun MediaStoreSongDao(): MediaStoreSongDao
    abstract fun PlaylistDao(): PlaylistDao
    abstract fun PlaylistSongDao(): PlaylistSongDao
}

object MusicDatabase {

    private var _songDatabase: SongDatabase? = null
    val songDatabase: SongDatabase
        get() {
            if (_songDatabase == null) {
                _songDatabase = Room.databaseBuilder(
                    App.instance,
                    SongDatabase::class.java,
                    DATABASE_NAME
                ).build()
            }
            return _songDatabase!!
        }

    suspend fun checkUpdate(context: Context) {
        DatabaseUpdater.checkAndRefresh(context.applicationContext, songDatabase)
    }


    /*
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
     */

    const val DATABASE_NAME = "music_v1.db"
    const val DATABASE_VERSION = 1
}