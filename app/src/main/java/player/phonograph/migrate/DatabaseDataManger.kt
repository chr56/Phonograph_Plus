/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.migrate

import okio.BufferedSink
import player.phonograph.provider.DatabaseConstants.FAVORITE_DB
import player.phonograph.provider.DatabaseConstants.HISTORY_DB
import player.phonograph.provider.DatabaseConstants.MUSIC_PLAYBACK_STATE_DB
import player.phonograph.provider.DatabaseConstants.PATH_FILTER
import player.phonograph.provider.DatabaseConstants.SONG_PLAY_COUNT_DB
import player.phonograph.util.FileUtil.createOrOverrideFile
import player.phonograph.util.FileUtil.moveFile
import player.phonograph.util.reportError
import player.phonograph.util.text.currentTimestamp
import player.phonograph.util.transferToOutputStream
import player.phonograph.util.zip.ZipUtil.addToZipFile
import player.phonograph.util.zip.ZipUtil.extractZipFile
import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DatabaseDataManger {

    fun exportDatabases(uri: Uri, context: Context): Boolean {
        return context.contentResolver.openFileDescriptor(uri, "w")?.use { parcel ->
            parcel.fileDescriptor?.let { fileDescriptor ->
                FileOutputStream(fileDescriptor).use {
                    exportDatabasesImpl(it, context)
                }
            }
        } ?: false
    }

    fun exportDatabases(sink: BufferedSink, dbName: String, context: Context): Boolean {
        val databaseFile = context.getDatabasePath(dbName) ?: return false
        val bytes = databaseFile.readBytes()
        sink.write(bytes)
        return true
    }

    fun exportDatabases(folder: String = "exported", context: Context) {
        context.getExternalFilesDir(folder)?.let {
            val name = "phonograph_plus_databases_${currentTimestamp()}.zip"
            FileOutputStream(File(it, name)).use { fileOutputStream ->
                exportDatabasesImpl(fileOutputStream, context)
            }
        }
    }

    private fun exportDatabasesImpl(fileOutputStream: FileOutputStream, context: Context): Boolean {
        ZipOutputStream(fileOutputStream).use { zipOut ->
            addToZipFile(zipOut, context.getDatabasePath(FAVORITE_DB), FAVORITE_DB)
            addToZipFile(zipOut, context.getDatabasePath(PATH_FILTER), PATH_FILTER)
            addToZipFile(zipOut, context.getDatabasePath(HISTORY_DB), HISTORY_DB)
            addToZipFile(zipOut, context.getDatabasePath(SONG_PLAY_COUNT_DB), SONG_PLAY_COUNT_DB)
            addToZipFile(zipOut, context.getDatabasePath(MUSIC_PLAYBACK_STATE_DB), MUSIC_PLAYBACK_STATE_DB)
        }
        return true // todo
    }

    fun importDatabases(uri: Uri, context: Context): Boolean {
        return context.contentResolver.openFileDescriptor(uri, "r")?.use { parcel ->
            parcel.fileDescriptor?.let { fd ->
                FileInputStream(fd).use {
                    importDatabaseImpl(it, context, context.cacheDir)
                }
            }
        } ?: false
    }

    fun importSingleDatabases(inputStream: InputStream, dbName: String, context: Context): Boolean {
        return try {
            // mahe cache
            val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "Backup_${currentTimestamp()}")
            if (cacheDir.exists()) {
                cacheDir.delete()
            } else {
                cacheDir.mkdirs()
            }
            importSingleDatabaseImpl(inputStream, dbName, context, cacheDir)
            cacheDir.delete()
            true
        } catch (e: Exception) {
            reportError(e, TAG,"Failed import database $dbName")
            e.printStackTrace()
            false
        }
    }

    private fun importSingleDatabaseImpl(
        inputStream: InputStream,
        dbName: String,
        context: Context,
        cacheDir: File,
    ) {

        val tmpFile = File(cacheDir, dbName).createOrOverrideFile()

        // copy from stream
        tmpFile.outputStream().use { fileOutputStream ->
            inputStream.transferToOutputStream(fileOutputStream)
        }

        // move
        moveFile(from = tmpFile, to = context.getDatabasePath(dbName))

        tmpFile.delete()
    }

    private fun importDatabaseImpl(
        fileInputStream: FileInputStream,
        context: Context,
        cacheDir: File,
    ): Boolean {
        if (!cacheDir.exists() && !cacheDir.isDirectory && !cacheDir.canWrite())
            throw FileNotFoundException("Output dirs unavailable!")
        ZipInputStream(fileInputStream).use { zipIn ->
            extractZipFile(zipIn, cacheDir)
        }
        if (cacheDir.exists()) {
            replaceDatabaseFile(cacheDir, context)
        }
        return true // todo
    }

    private fun replaceDatabaseFile(sourceDir: File, context: Context) {
        if (sourceDir.exists() && sourceDir.isDirectory) {
            moveFile(from = File(sourceDir, FAVORITE_DB), to = context.getDatabasePath(FAVORITE_DB))
            moveFile(from = File(sourceDir, PATH_FILTER), to = context.getDatabasePath(PATH_FILTER))
            moveFile(from = File(sourceDir, HISTORY_DB), to = context.getDatabasePath(HISTORY_DB))
            moveFile(from = File(sourceDir, SONG_PLAY_COUNT_DB), to = context.getDatabasePath(SONG_PLAY_COUNT_DB))
            moveFile(
                from = File(sourceDir, MUSIC_PLAYBACK_STATE_DB),
                to = context.getDatabasePath(MUSIC_PLAYBACK_STATE_DB)
            )
        }
    }

    private const val TAG = "DatabaseManger"
}
