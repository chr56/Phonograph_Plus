/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.backup

import okio.BufferedSink
import okio.IOException
import okio.Source
import okio.buffer
import okio.sink
import okio.source
import player.phonograph.util.file.createOrOverrideFile
import player.phonograph.util.file.moveFile
import player.phonograph.util.reportError
import player.phonograph.util.text.currentTimestamp
import player.phonograph.util.warning
import android.content.Context
import java.io.File

object DatabaseManger {

    fun export(context: Context, sink: BufferedSink, dbName: String): Boolean {
        val databaseFile = context.getDatabasePath(dbName) ?: return false
        return if (databaseFile.isFile && databaseFile.length() > 0) {
            sink.writeAll(databaseFile.source())
            true
        } else {
            warning(TAG, "Failed to export $dbName (empty)")
            false
        }
    }

    fun import(context: Context, source: Source, dbName: String): Boolean =
        try {
            val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "Backup_${currentTimestamp()}")
            synchronized(cacheDir) {
                // make cache directory
                if (cacheDir.exists()) cacheDir.delete() else cacheDir.mkdirs()
                // make temporary file
                val temp = File(cacheDir, dbName).createOrOverrideFile()
                temp.sink().buffer().use { buffer -> buffer.writeAll(source) }
                // move and replace
                moveFile(from = temp, to = context.getDatabasePath(dbName))
                temp.delete()
                cacheDir.delete()
            }
            true
        } catch (e: IOException) {
            reportError(e, TAG, "Failed import database $dbName")
            false
        }

    private const val TAG = "DatabaseManger"
}
