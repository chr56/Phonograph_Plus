/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.backup

import okio.Buffer
import okio.Source
import okio.buffer
import okio.sink
import okio.source
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.backup.RawDatabaseBackupItemExecutor.Companion.TAG
import player.phonograph.mechanism.migrate.PathFilterMigrationRule
import player.phonograph.model.backup.BackupItemExecutor
import player.phonograph.util.file.createOrOverrideFile
import player.phonograph.util.file.moveFile
import player.phonograph.util.text.currentTimestamp
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import java.io.File
import java.io.IOException

class RawDatabaseBackupItemExecutor(val databaseName: String) : BackupItemExecutor {

    override suspend fun export(context: Context): Buffer? {
        val databaseFile = context.getDatabasePath(databaseName) ?: return null
        return if (databaseFile.isFile && databaseFile.length() > 0) {
            Buffer().apply {
                writeAll(databaseFile.source())
            }
        } else {
            warning(context, TAG, "Empty database $databaseName to export")
            null
        }
    }

    override suspend fun import(context: Context, source: Source): Boolean =
        try {
            val cacheDir = File(context.externalCacheDir ?: context.cacheDir, "Backup_${currentTimestamp()}")
            synchronized(cacheDir) {
                // make cache directory
                if (cacheDir.exists()) cacheDir.delete() else cacheDir.mkdirs()
                // make temporary file
                val temp = File(cacheDir, databaseName).createOrOverrideFile()
                temp.sink().buffer().use { buffer -> buffer.writeAll(source) }
                // move and replace
                moveFile(from = temp, to = context.getDatabasePath(databaseName))
                temp.delete()
                cacheDir.delete()
            }
            true
        } catch (e: IOException) {
            warning(context, TAG, "Failed import database $databaseName", e)
            false
        }

    companion object {
        private const val TAG = "DatabaseManger"
    }
}

object LegacyPathFilterDatabaseBackupItemExecutor : BackupItemExecutor {
    // import only
    override suspend fun export(context: Context): Buffer? {
        return null
    }

    override suspend fun import(context: Context, source: Source): Boolean {
        val file = File(
            context.externalCacheDir ?: context.cacheDir,
            "PathFilter.Backup_${currentTimestamp()}.db"
        )
        try {
            file.createOrOverrideFile()
            file.sink().buffer().use { buffer -> buffer.writeAll(source) }
        } catch (e: IOException) {
            warning(context, "PathFilterDatabaseBackup", "Failed to create $file", e)
            return false
        }

        try {
            SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                PathFilterMigrationRule().import(context, db)
            }
            if (file.exists()) file.delete()
        } catch (e: SQLiteException) {
            warning(context, "PathFilterDatabaseBackup", "Failed to open $file", e)
            return false
        } catch (e: IOException) {
            warning(context, "PathFilterDatabaseBackup", "Failed to process $file", e)
            return false
        }

        return true
    }
}