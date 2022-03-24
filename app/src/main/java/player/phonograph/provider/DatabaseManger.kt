/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.provider

import android.content.Context
import android.net.Uri
import player.phonograph.provider.DatabaseConstants.BLACKLIST_DB
import player.phonograph.provider.DatabaseConstants.FAVORITE_DB
import player.phonograph.provider.DatabaseConstants.HISTORY_DB
import player.phonograph.provider.DatabaseConstants.MUSIC_PLAYBACK_STATE_DB
import player.phonograph.provider.DatabaseConstants.SONG_PLAY_COUNT_DB
import player.phonograph.util.Util.assertIfFalse
import player.phonograph.util.Util.currentTimestamp
import java.io.*
import java.lang.IllegalArgumentException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DatabaseManger(var context: Context) {

    fun exportDatabases(uri: Uri) {
        context.contentResolver.openFileDescriptor(uri, "w")?.fileDescriptor?.let { fileDescriptor ->
            FileOutputStream(fileDescriptor).use {
                exportDatabasesImpl(it)
            }
        }
    }

    fun exportDatabases(folder: String = "exported") {
        context.getExternalFilesDir(folder)?.let {
            FileOutputStream(File(it, "phonograph_plus_databases_${currentTimestamp()}.zip")).use { fileOutputStream ->
                exportDatabasesImpl(fileOutputStream)
            }
        }
    }

    private fun exportDatabasesImpl(fileOutputStream: FileOutputStream) {
        ZipOutputStream(fileOutputStream).use { zipOut ->
            addToZipFile(zipOut, context.getDatabasePath(FAVORITE_DB), FAVORITE_DB)
            addToZipFile(zipOut, context.getDatabasePath(BLACKLIST_DB), BLACKLIST_DB)
            addToZipFile(zipOut, context.getDatabasePath(HISTORY_DB), HISTORY_DB)
            addToZipFile(zipOut, context.getDatabasePath(SONG_PLAY_COUNT_DB), SONG_PLAY_COUNT_DB)
            addToZipFile(zipOut, context.getDatabasePath(MUSIC_PLAYBACK_STATE_DB), MUSIC_PLAYBACK_STATE_DB)
        }
    }

    fun importDatabases(uri: Uri) {
        context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor?.let { fd ->
            FileInputStream(fd).use {
                importDatabaseImpl(it, context.cacheDir)
            }
        }
    }

    private fun importDatabaseImpl(fileInputStream: FileInputStream, cacheDir: File) {
        if (!cacheDir.exists() && !cacheDir.isDirectory && !cacheDir.canWrite())
            throw FileNotFoundException("Output dirs unavailable!")
        ZipInputStream(fileInputStream).use { zipIn ->
            extractZipFile(zipIn, cacheDir)
        }
        if (cacheDir.exists()) {
            replaceDatabaseFile(cacheDir)
        }
    }

    private fun replaceDatabaseFile(sourceDir: File) {
        if (sourceDir.exists() && sourceDir.isDirectory) {
            moveFile(from = File(sourceDir, FAVORITE_DB), to = context.getDatabasePath(FAVORITE_DB))
            moveFile(from = File(sourceDir, BLACKLIST_DB), to = context.getDatabasePath(BLACKLIST_DB))
            moveFile(from = File(sourceDir, HISTORY_DB), to = context.getDatabasePath(HISTORY_DB))
            moveFile(from = File(sourceDir, SONG_PLAY_COUNT_DB), to = context.getDatabasePath(SONG_PLAY_COUNT_DB))
            moveFile(from = File(sourceDir, MUSIC_PLAYBACK_STATE_DB), to = context.getDatabasePath(MUSIC_PLAYBACK_STATE_DB))
        }
    }

    private fun moveFile(from: File, to: File) {
        if (from.isDirectory || to.isDirectory) throw IllegalArgumentException("move dirs")
        if (from.exists() && from.canWrite()) {
            to.delete().assertIfFalse(IOException("Can't delete $BLACKLIST_DB"))
            from.renameTo(to).assertIfFalse(IOException("Can't replace file $BLACKLIST_DB"))
        }
    }

    private fun addToZipFile(destination: ZipOutputStream, file: File, entryName: String) {
        if (file.exists() && file.isFile) {
            destination.putNextEntry(ZipEntry(entryName))
            BufferedInputStream(FileInputStream(file)).use { fs ->
                val buffer = ByteArray(1024)
                var len: Int
                while (fs.read(buffer).also { len = it } != -1) {
                    destination.write(buffer, 0, len)
                }
            }
        } // todo else
    }

    private fun extractZipFile(source: ZipInputStream, destinationDir: File) {
        var entry: ZipEntry?
        while (source.nextEntry.also { entry = it } != null) {
            if (entry == null) throw IOException("Zip file has no entry")
            if (!entry!!.isDirectory) {
                val file = File(destinationDir, entry!!.name)
                FileOutputStream(file).use { fos ->
                    BufferedOutputStream(fos).use { outputStream ->
                        var len: Int
                        val bytes = ByteArray(1024)
                        while (source.read(bytes).also { len = it } != -1) {
                            outputStream.write(bytes, 0, len)
                        }
                    }
                }
            } // todo else
        }
    }
}
