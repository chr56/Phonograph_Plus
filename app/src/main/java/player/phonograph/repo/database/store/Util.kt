/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.database.store

import player.phonograph.App
import player.phonograph.foundation.error.warning
import player.phonograph.util.debug
import android.database.sqlite.SQLiteDatabase
import android.util.Log


interface Cleanable {
    fun gc(idsExists: List<Long>)
}

/**
 * clean unavailable entries for table([tableName]) in [database]
 * @param locked entries that should not be deleted
 * @param columnName where are the [locked] from
 */
fun gc(database: SQLiteDatabase, tableName: String, columnName: String, locked: Array<String>) {

    val count = locked.size

    val selectionPlaceHolder = when {
        count > 1  -> "?" + ",?".repeat(count - 1)
        count == 1 -> "?"
        else       -> return
    }

    val selection = "$columnName NOT IN ( $selectionPlaceHolder )"

    database.beginTransaction()
    try {
        database.delete(tableName, selection, locked).let {
            debug {
                Log.v(tableName, "Database $tableName GC: $it columns removed")
            }
        }
        database.setTransactionSuccessful()
    } catch (e: Exception) {
        warning(App.instance, tableName, "Failed to clean up", e)
    } finally {
        database.endTransaction()
    }
}