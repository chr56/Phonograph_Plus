/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.migrate.backup

import okio.Buffer
import player.phonograph.App
import player.phonograph.migrate.DatabaseBackupManger
import player.phonograph.migrate.DatabaseDataManger
import player.phonograph.migrate.SettingDataManager
import player.phonograph.provider.DatabaseConstants
import java.io.InputStream

sealed class BackupItem(
    val key: String,
    val name: String,
    val type: Type,
) {
    enum class Type(val suffix: String) {
        BINARY("bin"),
        JSON("json"),
        DATABASE("db");
    }

    abstract fun data(): InputStream
}

object SettingBackup : BackupItem(KEY_SETTING, KEY_SETTING, Type.JSON) {
    override fun data(): InputStream {
        val buffer = Buffer()

        SettingDataManager.exportSettings(buffer)

        return buffer.inputStream()
    }
}

object PathFilterBackup : BackupItem(KEY_PATH_FILTER, KEY_PATH_FILTER, Type.JSON) {
    override fun data(): InputStream {
        val buffer = Buffer()

        DatabaseBackupManger.exportPathFilter(buffer, App.instance)

        return buffer.inputStream()
    }
}

object FavoriteBackup : BackupItem(KEY_FAVORITES, KEY_FAVORITES, Type.JSON) {
    override fun data(): InputStream {
        val buffer = Buffer()

        DatabaseBackupManger.exportFavorites(buffer, App.instance)

        return buffer.inputStream()
    }
}

object PlayingQueuesBackup : BackupItem(KEY_PLAYING_QUEUES, KEY_PLAYING_QUEUES, Type.JSON) {
    override fun data(): InputStream {
        val buffer = Buffer()

        DatabaseBackupManger.exportPlayingQueues(buffer, App.instance)

        return buffer.inputStream()
    }
}


object FavoriteDatabaseBackup : BackupItem(
    KEY_DATABASE_FAVORITE,
    KEY_DATABASE_FAVORITE,
    Type.DATABASE
) {
    override fun data(): InputStream {
        val buffer = Buffer()
        DatabaseDataManger.exportDatabases(buffer, DatabaseConstants.FAVORITE_DB, App.instance)
        return buffer.inputStream()
    }
}

object PathFilterDatabaseBackup : BackupItem(
    KEY_DATABASE_PATH_FILTER,
    KEY_DATABASE_PATH_FILTER,
    Type.DATABASE
) {
    override fun data(): InputStream {
        val buffer = Buffer()
        DatabaseDataManger.exportDatabases(buffer, DatabaseConstants.PATH_FILTER, App.instance)
        return buffer.inputStream()
    }
}

object HistoryDatabaseBackup : BackupItem(
    KEY_DATABASE_HISTORY,
    KEY_DATABASE_HISTORY,
    Type.DATABASE
) {
    override fun data(): InputStream {
        val buffer = Buffer()
        DatabaseDataManger.exportDatabases(buffer, DatabaseConstants.HISTORY_DB, App.instance)
        return buffer.inputStream()
    }
}

object SongPlayCountDatabaseBackup : BackupItem(
    KEY_DATABASE_SONG_PLAY_COUNT,
    KEY_DATABASE_SONG_PLAY_COUNT,
    Type.DATABASE
) {
    override fun data(): InputStream {

        val buffer = Buffer()
        DatabaseDataManger.exportDatabases(buffer, DatabaseConstants.SONG_PLAY_COUNT_DB, App.instance)
        return buffer.inputStream()
    }
}

object MusicPlaybackStateDatabaseBackup : BackupItem(
    KEY_DATABASE_MUSIC_PLAYBACK_STATE,
    KEY_DATABASE_MUSIC_PLAYBACK_STATE,
    Type.DATABASE
) {
    override fun data(): InputStream {
        val buffer = Buffer()
        DatabaseDataManger.exportDatabases(buffer, DatabaseConstants.MUSIC_PLAYBACK_STATE_DB, App.instance)
        return buffer.inputStream()
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
