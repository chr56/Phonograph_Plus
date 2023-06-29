/*
 *  Copyright (c) 2022~2023 chr_56
 */
package player.phonograph.repo.database

import player.phonograph.repo.database.DatabaseConstants.HISTORY_DB
import player.phonograph.repo.database.HistoryStore.RecentStoreColumns.Companion.ID
import player.phonograph.repo.database.HistoryStore.RecentStoreColumns.Companion.NAME
import player.phonograph.repo.database.HistoryStore.RecentStoreColumns.Companion.TIME_PLAYED
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HistoryStore(context: Context) :
        SQLiteOpenHelper(context, HISTORY_DB, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $NAME" +
                    " ($ID LONG NOT NULL,$TIME_PLAYED LONG NOT NULL);"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $NAME")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $NAME")
        onCreate(db)
    }

    fun addSongId(songId: Long) {
        if (songId == -1L) {
            return
        }
        val database = writableDatabase
        database.beginTransaction()
        try {
            // remove previous entries
            removeSongId(songId)

            // add the entry
            database.insert(
                NAME, null,
                ContentValues(2).apply {
                    put(ID, songId)
                    put(TIME_PLAYED, System.currentTimeMillis())
                }
            )

            // if our db is too large, delete the extra items
            runCatching {
                database.query(
                    NAME, arrayOf(TIME_PLAYED), null, null, null, null,
                    "$TIME_PLAYED ASC"
                ).use { oldest: Cursor? ->
                    if (oldest != null && oldest.count > MAX_ITEMS_IN_DB) {
                        oldest.moveToPosition(oldest.count - MAX_ITEMS_IN_DB)
                        val timeOfRecordToKeep = oldest.getLong(0)
                        database.delete(
                            NAME, "$TIME_PLAYED < ?", arrayOf(timeOfRecordToKeep.toString())
                        )
                    }
                }
            }
        } finally {
            database.setTransactionSuccessful()
            database.endTransaction()
        }
    }

    fun removeSongId(songId: Long) {
        writableDatabase.delete(NAME, "$ID = ?", arrayOf(songId.toString()))
    }

    fun clear() {
        writableDatabase.delete(NAME, null, null)
    }

    operator fun contains(id: Long): Boolean =
        readableDatabase.query(
            NAME, arrayOf(ID),
            "$ID=?", arrayOf(id.toString()),
            null, null, null, null
        ).use { cursor ->
            cursor != null && cursor.moveToFirst()
        }

    fun queryRecentIds(): Cursor =
        readableDatabase.query(
            NAME, arrayOf(ID), null, null, null, null,
            "$TIME_PLAYED DESC"
        )

    fun gc(idsExists: List<Long>) {
        gc(writableDatabase, NAME, ID, idsExists.map { it.toString() }.toTypedArray())
    }

    interface RecentStoreColumns {
        companion object {
            const val NAME = "recent_history"

            const val ID = "song_id"
            const val TIME_PLAYED = "time_played"
        }
    }

    companion object {

        private var sInstance: HistoryStore? = null
        @Synchronized
        fun getInstance(context: Context): HistoryStore {
            if (sInstance == null) {
                sInstance = HistoryStore(context.applicationContext)
            }
            return sInstance!!
        }

        private const val MAX_ITEMS_IN_DB = 150
        private const val VERSION = 1
    }
}
