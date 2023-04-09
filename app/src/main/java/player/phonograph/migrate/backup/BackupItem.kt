/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.migrate.backup

import okio.Buffer
import okio.BufferedSink
import player.phonograph.migrate.DatabaseBackupManger
import player.phonograph.migrate.DatabaseDataManger
import player.phonograph.migrate.SettingDataManager
import player.phonograph.provider.DatabaseConstants
import android.content.Context
import android.content.res.Resources
import java.io.InputStream

sealed class BackupItem(
    val key: String,
    val type: Type,
) {
    abstract fun data(context: Context): InputStream

    open fun displayName(resources: Resources): CharSequence = key

    /**
     * Type of Backup
     */
    enum class Type(val suffix: String) {
        BINARY("bin"),
        JSON("json"),
        DATABASE("db");
    }
}

private const val KEY_SETTING = "setting"
private const val KEY_PATH_FILTER = "path_filter"
private const val KEY_FAVORITES = "favorite"
private const val KEY_PLAYING_QUEUES = "playing_queues"
private const val KEY_DATABASE_FAVORITE = "database_favorite"
private const val KEY_DATABASE_PATH_FILTER = "database_path_filter"
private const val KEY_DATABASE_HISTORY = "database_history"
private const val KEY_DATABASE_SONG_PLAY_COUNT = "database_song_play_count"
private const val KEY_DATABASE_MUSIC_PLAYBACK_STATE = "database_music_playback_state"

private fun fromSink(block: (BufferedSink) -> Boolean): InputStream {
    val buffer = Buffer()
    block(buffer)
    return buffer.inputStream()
}


object SettingBackup : BackupItem(KEY_SETTING, Type.JSON) {
    override fun data(context: Context): InputStream =
        fromSink {
            SettingDataManager.exportSettings(it)
        }
}

object PathFilterBackup : BackupItem(KEY_PATH_FILTER, Type.JSON) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseBackupManger.exportPathFilter(it, context)
        }
}

object FavoriteBackup : BackupItem(KEY_FAVORITES, Type.JSON) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseBackupManger.exportFavorites(it, context)
        }
}

object PlayingQueuesBackup : BackupItem(KEY_PLAYING_QUEUES, Type.JSON) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseBackupManger.exportPlayingQueues(it, context)
        }
}


object FavoriteDatabaseBackup : BackupItem(KEY_DATABASE_FAVORITE, Type.DATABASE) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseDataManger.exportDatabases(it, DatabaseConstants.FAVORITE_DB, context)
        }
}

object PathFilterDatabaseBackup : BackupItem(KEY_DATABASE_PATH_FILTER, Type.DATABASE) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseDataManger.exportDatabases(it, DatabaseConstants.PATH_FILTER, context)
        }
}

object HistoryDatabaseBackup : BackupItem(KEY_DATABASE_HISTORY, Type.DATABASE) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseDataManger.exportDatabases(it, DatabaseConstants.HISTORY_DB, context)
        }
}

object SongPlayCountDatabaseBackup : BackupItem(KEY_DATABASE_SONG_PLAY_COUNT, Type.DATABASE) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseDataManger.exportDatabases(it, DatabaseConstants.SONG_PLAY_COUNT_DB, context)
        }
}

object MusicPlaybackStateDatabaseBackup : BackupItem(KEY_DATABASE_MUSIC_PLAYBACK_STATE, Type.DATABASE) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseDataManger.exportDatabases(it, DatabaseConstants.MUSIC_PLAYBACK_STATE_DB, context)
        }
}
