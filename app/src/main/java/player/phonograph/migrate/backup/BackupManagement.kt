/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.migrate.backup

import player.phonograph.util.FileUtil.createOrOverrideFile
import player.phonograph.util.text.currentDateTime
import player.phonograph.util.text.currentTimestamp
import player.phonograph.util.transferToOutputStream
import player.phonograph.util.zip.ZipUtil.addToZipFile
import android.content.Context
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipOutputStream

object Backup {
    // todo
    private val DEFAULT_BACKUP_CONFIG =
        listOf(
            SettingBackup, FavoriteBackup, PathFilterBackup, PlayingQueuesBackup,
            FavoriteDatabaseBackup,
            PathFilterDatabaseBackup,
            HistoryDatabaseBackup,
            SongPlayCountDatabaseBackup,
            MusicPlaybackStateDatabaseBackup,
        )

    fun exportBackupToArchive(
        context: Context,
        config: List<BackupItem> = DEFAULT_BACKUP_CONFIG,
        targetOutputStream: OutputStream,
    ) {

        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val tmpDir = File(cacheDir, "BackupTmp_${currentTimestamp()}").also { it.mkdirs() }
        exportBackupToDirectory(context, config, tmpDir)

        ZipOutputStream(targetOutputStream).use { zip ->
            tmpDir.listFiles()?.forEach {
                addToZipFile(zip, it, it.name)
            }
        }

        tmpDir.deleteRecursively()
    }


    private fun exportBackupToDirectory(
        context: Context,
        config: List<BackupItem>,
        destination: File,
    ) {
        val time = currentDateTime()

        val manifest = File(destination, BACKUP_MANIFEST_FILE).createOrOverrideFile()
        val manifestWriter = manifest.bufferedWriter()

        manifestWriter.line("$BACKUP_TIME=${currentTimestamp()}")

        for (item in config) {
            val filename = "phonograph_plus_backup_${item.key}_$time.${item.type.suffix}"
            val file = File(destination, filename).createOrOverrideFile()
            file.outputStream().use { outputStream ->
                item.data(context).use { inputStream ->
                    inputStream.transferToOutputStream(outputStream)
                }
            }
            manifestWriter.line("${item.key}=$filename")
        }

        manifestWriter.close()
    }

    private fun BufferedWriter.line(string: String): BufferedWriter {
        write("$string\n")
        return this
    }

    private const val BACKUP_MANIFEST_FILE = "MANIFEST.property"
    private const val BACKUP_TIME = "BackupTime"
}