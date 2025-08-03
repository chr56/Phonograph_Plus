/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.migrate

import player.phonograph.foundation.error.warning
import player.phonograph.model.migration.VersionMigrationRule
import player.phonograph.settings.PathFilterSetting
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException


class PathFilterMigrationRule : UserDataMigrationRule(introduced = 1102) {

    override fun execute(context: Context) {
        withDatabase(context, DATABASE_NAME_PATH_FILTER, ::import)
    }

    fun import(context: Context, db: SQLiteDatabase) {
        importPathsFromDatabase(context, db, DATABASE_TABLE_EXCLUDE_LIST, excludeMode = true)
        importPathsFromDatabase(context, db, DATABASE_TABLE_INCLUDE_LIST, excludeMode = false)
    }

    private fun importPathsFromDatabase(
        context: Context,
        db: SQLiteDatabase,
        table: String,
        excludeMode: Boolean,
    ) {
        val existed = try {
            readPaths(db, table)
        } catch (e: Exception) {
            warning(context, TAG, "Failed to read table $table", e)
            return
        }
        if (existed.isEmpty()) return
        try {
            CoroutineScope(Dispatchers.IO).launch { PathFilterSetting.add(context, excludeMode, existed) }
        } catch (e: IOException) {
            warning(
                context, TAG,
                "Failed to import PathFilter (${if (excludeMode) "Exclude" else "Include"} Mode)", e
            )
        }
    }

    private fun readPaths(db: SQLiteDatabase, table: String): List<String> {
        val paths = mutableListOf<String>()
        db.query(
            table, arrayOf(DATABASE_COLUMN_PATH),
            null, null, null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    paths.add(cursor.getString(0))
                } while (cursor.moveToNext())
            }
        }
        return paths.toList()
    }

    override fun check(context: Context, from: Int, to: Int): Boolean {
        val databaseFile = context.getDatabasePath(DATABASE_NAME_PATH_FILTER)
        val exists = try {
            databaseFile.exists()
        } catch (_: Exception) {
            false
        }
        return super.check(context, from, to) && exists
    }

    companion object {
        private const val DATABASE_NAME_PATH_FILTER = "blacklist.db"
        private const val DATABASE_TABLE_EXCLUDE_LIST = "blacklist"
        private const val DATABASE_TABLE_INCLUDE_LIST = "whitelist"
        private const val DATABASE_COLUMN_PATH = "path"

        private const val TAG = "PathFilterMigrationRule"
    }
}


sealed class UserDataMigrationRule(introduced: Int) : VersionMigrationRule(introduced) {

    /**
     * @param name database name
     */
    protected fun withDatabase(
        context: Context,
        name: String,
        action: (Context, SQLiteDatabase) -> Unit,
    ) {
        val databaseFile = context.getDatabasePath(name)
        try {
            if (databaseFile.exists()) {
                SQLiteDatabase.openDatabase(databaseFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                    action(context, db)
                }
            } else {
                warning(context, TAG, "Database $name has been already migrated?")
            }
        } catch (e: SQLiteException) {
            warning(context, TAG, "Failed to open database $name", e)
        } catch (e: IOException) {
            warning(context, TAG, "Failed to access database $name", e)
        }
    }

    protected fun removeDatabase(context: Context, name: String, delete: Boolean) {
        val backupDestination = context.getExternalFilesDir(null)
        val databaseFile = context.getDatabasePath(name)
        try {
            if (!databaseFile.exists()) {
                Log.i(TAG, "Database $name does not exist...")
                return
            }
            if (backupDestination != null) {
                val backupFile = File(backupDestination, "$name.${System.currentTimeMillis()}.bkp")
                if (!databaseFile.renameTo(backupFile)) {
                    if (delete) context.deleteDatabase(name)
                }
            } else {
                if (delete) context.deleteDatabase(name)
            }
        } catch (e: IOException) {
            warning(context, TAG, "Failed to delete legacy database $name", e)
        }
    }

    companion object {
        private const val TAG = "UserDataMigrationRule"
    }
}
