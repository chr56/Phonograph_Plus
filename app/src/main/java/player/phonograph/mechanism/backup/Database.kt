/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.backup

import okio.BufferedSink
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
            // mahe cache
            val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "Backup_${currentTimestamp()}")
            if (cacheDir.exists()) {
                cacheDir.delete()
            } else {
                cacheDir.mkdirs()
            }
            importDatabaseImpl(source, dbName, context, cacheDir)
            cacheDir.delete()
            true
        } catch (e: Exception) {
            reportError(e, TAG, "Failed import database $dbName")
            e.printStackTrace()
            false
        }

    private fun importDatabaseImpl(
        source: Source,
        dbName: String,
        context: Context,
        cacheDir: File,
    ) {

        val tmpFile = File(cacheDir, dbName).createOrOverrideFile()

        // copy from stream
        tmpFile.sink().buffer().use { buffer ->
            buffer.writeAll(source)
        }

        // move
        moveFile(from = tmpFile, to = context.getDatabasePath(dbName))

        // dlete
        tmpFile.delete()
    }

    private const val TAG = "DatabaseManger"
}
