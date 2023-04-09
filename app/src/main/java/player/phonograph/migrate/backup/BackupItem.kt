/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.migrate.backup

import okio.Buffer
import player.phonograph.App
import player.phonograph.migrate.DatabaseBackupManger
import player.phonograph.migrate.SettingDataManager
import java.io.InputStream

sealed class BackupItem(
    val key: String,
    val name: String,
) {
    abstract fun data(): InputStream
}

object SettingBackup : BackupItem(KEY_SETTING, KEY_SETTING) {
    override fun data(): InputStream {
        val buffer = Buffer()

        SettingDataManager.exportSettings(buffer)

        return buffer.inputStream()
    }
}

object PathFilterBackup : BackupItem(KEY_PATH_FILTER, KEY_PATH_FILTER) {
    override fun data(): InputStream {
        val buffer = Buffer()

        DatabaseBackupManger.exportPathFilter(buffer, App.instance)

        return buffer.inputStream()
    }
}

object FavoriteBackup : BackupItem(KEY_FAVORITES, KEY_FAVORITES) {
    override fun data(): InputStream {
        val buffer = Buffer()

        DatabaseBackupManger.exportFavorites(buffer, App.instance)

        return buffer.inputStream()
    }
}
object PlayingQueuesBackup : BackupItem(KEY_PLAYING_QUEUES, KEY_PLAYING_QUEUES) {
    override fun data(): InputStream {
        val buffer = Buffer()

        DatabaseBackupManger.exportPlayingQueues(buffer, App.instance)

        return buffer.inputStream()
    }
}

private const val KEY_SETTING = "setting"
private const val KEY_PATH_FILTER = "path_filter"
private const val KEY_FAVORITES = "favorite"
private const val KEY_PLAYING_QUEUES = "playing_queues"
