/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.migrate.backup

import player.phonograph.util.FileUtil.createOrOverrideFile
import player.phonograph.util.text.currentDateTime
import player.phonograph.util.text.currentTimestamp
import player.phonograph.util.transferToOutputStream
import player.phonograph.util.zip.ZipUtil.addToZipFile
import player.phonograph.util.zip.ZipUtil.extractZipFile
import android.content.Context
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream
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

    fun importBackupFromArchive(
        context: Context,
        sourceInputStream: InputStream,
    ) {
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val tmpDir = File(cacheDir, "BackupTmp_${currentTimestamp()}").also { it.mkdirs() }

        ZipInputStream(sourceInputStream).use { zipIn ->
            extractZipFile(zipIn, tmpDir)
            importBackupFromDirectory(context, tmpDir)
        }

        tmpDir.deleteRecursively()
    }

    private fun importBackupFromDirectory(
        context: Context,
        source: File,
    ) {
        require(source.exists() && source.isDirectory) { "${source.absolutePath} is not accessible directory!" }
        // read manifest
        val manifestFile = File(source, BACKUP_MANIFEST_FILE)
        if (manifestFile.exists() && manifestFile.isFile) {
            val manifest = readManifest(source, manifestFile)
            for ((item, file) in manifest.files) {
                FileInputStream(file).use { inputStream ->
                    item.import(inputStream, context)
                }
            }
        } else {
            throw IllegalStateException("No manifest found!") //todo: implement no-manifest-import
        }
    }

    private fun readManifest(source: File, manifestFile: File): ManifestFile {
        val manifest =
            manifestFile.readLines()
                .map { it.split("=", ignoreCase = true, limit = 2) }
                .associate { Pair(it[0], it[1]) }
        val timestamp = (manifest[BACKUP_TIME] ?: "0").toLong()
        val files = manifest.entries.mapNotNull { (key, value) ->
            val backupItem = BackupItem.fromKey(key)
            if (backupItem == null)
                null
            else
                Pair(backupItem, File(source, value))
        }.toMap()
        return ManifestFile(timestamp, files)
    }

    private fun BufferedWriter.line(string: String): BufferedWriter {
        write("$string\n")
        return this
    }

    private const val BACKUP_MANIFEST_FILE = "MANIFEST.property"
    private const val BACKUP_TIME = "BackupTime"

    private class ManifestFile(
        val timestamp: Long,
        val files: Map<BackupItem, File>,
    )
}