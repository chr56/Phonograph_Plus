/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.provider

import android.content.Context
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import player.phonograph.provider.DatabaseConstants.BLACKLIST_DB
import player.phonograph.provider.DatabaseConstants.FAVORITE_DB
import player.phonograph.provider.DatabaseConstants.HISTORY_DB
import player.phonograph.provider.DatabaseConstants.MUSIC_PLAYBACK_STATE_DB
import player.phonograph.provider.DatabaseConstants.SONG_PLAY_COUNT_DB
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DatabaseManger(var context: Context) {

    fun exportDatabases() {
        val path = context.getExternalFilesDir("exported")

        FileOutputStream(File(path, "phonograph_plus_databases_$timestamp.zip")).use { fileOutputStream ->
            ZipOutputStream(fileOutputStream).use { zipOut ->
                addToZipFile(zipOut, context.getDatabasePath(BLACKLIST_DB), BLACKLIST_DB)
                addToZipFile(zipOut, context.getDatabasePath(FAVORITE_DB), FAVORITE_DB)
                addToZipFile(zipOut, context.getDatabasePath(HISTORY_DB), HISTORY_DB)
                addToZipFile(zipOut, context.getDatabasePath(SONG_PLAY_COUNT_DB), SONG_PLAY_COUNT_DB)
                addToZipFile(zipOut, context.getDatabasePath(MUSIC_PLAYBACK_STATE_DB), MUSIC_PLAYBACK_STATE_DB)
            }
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

    private val timestamp: Long by lazy {
        Calendar.getInstance(TimeZone.getDefault(), ULocale.getDefault()).timeInMillis
    }
}
