/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.migrate.backup

import player.phonograph.util.FileUtil.createOrOverrideFile
import player.phonograph.util.text.currentDateTime
import player.phonograph.util.transferToOutputStream
import android.content.Context
import android.os.Environment
import java.io.File

object Backup {
    // todo
    private val DEFAULT_BACKUP_CONFIG =
        listOf(SettingBackup, FavoriteBackup, PathFilterBackup, PlayingQueuesBackup)

    fun exportBackupToDirectory(
        context: Context,
        config: List<BackupItem> = DEFAULT_BACKUP_CONFIG,
        destination: File = context.getExternalFilesDir("Backup") ?: File(Environment.DIRECTORY_DOWNLOADS),
    ) {
        val time = currentDateTime()
        for (item in config) {
            val file = File(destination, "phonograph_plus_backup_${item.key}_$time.json").createOrOverrideFile()
            file.outputStream().use { outputStream ->
                item.data().use { inputStream ->
                    inputStream.transferToOutputStream(outputStream)
                }
            }
        }
    }
}