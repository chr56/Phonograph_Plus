/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import player.phonograph.App
import player.phonograph.model.Song

class FavoriteSongsStore(context: Context = App.instance) : SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_NAME ($COLUMNS_ID LONG NOT NULL PRIMARY KEY, $COLUMNS_PATH TEXT NOT NULL, $COLUMNS_TITLE TEXT);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME") // todo
        onCreate(db)
    }

    fun clear() {
        val database = writableDatabase
        database.delete(TABLE_NAME, null, null)
    }

    fun contains(song: Song): Boolean = contains(song.id, song.data)

    fun contains(songId: Long?, path: String?): Boolean {
        val database = readableDatabase
        val cursor = database.query(
            TABLE_NAME,
            arrayOf(COLUMNS_ID, COLUMNS_PATH, COLUMNS_TITLE),
            "$COLUMNS_ID =? OR $COLUMNS_PATH =?",
            arrayOf(songId?.toString() ?: "0", path ?: ""),
            null, null, null,
        )
        var result: Boolean = false
        cursor.use {
            result = cursor.moveToFirst()
        }
        return result
    }

    fun add(song: Song): Boolean {
        val database = writableDatabase
        var result = false

        database.beginTransaction()
        try {
            val values = ContentValues(3)
            values.put(COLUMNS_ID, song.id)
            values.put(COLUMNS_PATH, song.data)
            values.put(COLUMNS_TITLE, song.title)

            database.insert(TABLE_NAME, null, values)

            database.setTransactionSuccessful()
            result = true
        } finally {
            database.endTransaction()
        }

        return result
    }

    fun remove(songId: Long, path: String): Boolean {
        val database = writableDatabase
        var result = false

        database.beginTransaction()
        try {

            database.delete(
                TABLE_NAME,
                "$COLUMNS_ID =? AND $COLUMNS_PATH =?",
                arrayOf(songId.toString(), path)
            ).let {
                result = it > 0
            }

            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        return result
    }
    fun remove(song: Song): Boolean = remove(song.id, song.data)

    companion object {
        private const val VERSION = 1
        private const val DATABASE_NAME = "favorite.db"

        private const val TABLE_NAME = "songs"

        const val COLUMNS_ID = "id" // long
        const val COLUMNS_PATH = "path" // string
        const val COLUMNS_TITLE = "title" // string

        private var mInstance: FavoriteSongsStore? = null
        val instance: FavoriteSongsStore
            get() {
                if (mInstance == null) {
                    mInstance = FavoriteSongsStore(App.instance)
                }
                return mInstance!!
            }
    }
}
