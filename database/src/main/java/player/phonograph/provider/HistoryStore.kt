/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package player.phonograph.provider

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import player.phonograph.provider.DatabaseConstants.HISTORY_DB
import player.phonograph.provider.HistoryStore.RecentStoreColumns.Companion.ID
import player.phonograph.provider.HistoryStore.RecentStoreColumns.Companion.NAME
import player.phonograph.provider.HistoryStore.RecentStoreColumns.Companion.TIME_PLAYED

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
