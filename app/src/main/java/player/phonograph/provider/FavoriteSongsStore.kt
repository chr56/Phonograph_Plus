/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.provider

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import player.phonograph.App
import player.phonograph.model.Song
import player.phonograph.provider.DatabaseConstants.FAVORITE_DB
import player.phonograph.service.MusicService
import player.phonograph.util.MediaStoreUtil
import player.phonograph.util.Util.currentTimestamp

class FavoriteSongsStore(context: Context = App.instance) : SQLiteOpenHelper(context, FAVORITE_DB, null, VERSION) {

    private val creatingTableSQL =
        "CREATE TABLE IF NOT EXISTS $TABLE_NAME ($COLUMNS_ID LONG NOT NULL PRIMARY KEY, $COLUMNS_PATH TEXT NOT NULL, $COLUMNS_TITLE TEXT, $COLUMNS_TIMESTAMP LONG);"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(creatingTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME") // todo
        onCreate(db)
    }

    fun clear() {
        val database = writableDatabase
        database.delete(TABLE_NAME, null, null)
        notifyMediaStoreChanged()
    }

    fun getAllSongs(context: Context): List<Song> {
        val result: MutableList<Song> = ArrayList()
        for (item in getAll()) {
            val song = MediaStoreUtil.getSong(context, item.first)
            if (song != Song.EMPTY_SONG) result.add(song)
        }
        return result
    }

    fun getAll(): List<Pair<Long, String>> {
        val database = readableDatabase
        val cursor = database.query(
            TABLE_NAME,
            arrayOf(COLUMNS_ID, COLUMNS_PATH, COLUMNS_TITLE, COLUMNS_TIMESTAMP),
            null, null, null, null, "$COLUMNS_TIMESTAMP DESC"
        )
        val result: MutableList<Pair<Long, String>> = ArrayList()
        cursor.use {
            cursor.moveToFirst().let { if (!it) return@use }
            do {
                result.add(
                    Pair(cursor.getLong(0), cursor.getString(1))
                )
            } while (cursor.moveToNext())
        }
        return result
    }

    fun contains(song: Song): Boolean = contains(song.id, song.data)

    fun contains(songId: Long?, path: String?): Boolean {
        val database = readableDatabase
        val cursor = database.query(
            TABLE_NAME,
            arrayOf(COLUMNS_ID, COLUMNS_PATH, COLUMNS_TITLE, COLUMNS_TIMESTAMP),
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
            values.put(COLUMNS_TIMESTAMP, currentTimestamp())

            database.insert(TABLE_NAME, null, values)

            database.setTransactionSuccessful()
            notifyMediaStoreChanged()
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
            notifyMediaStoreChanged()
        } finally {
            database.endTransaction()
        }

        return result
    }
    fun remove(song: Song): Boolean = remove(song.id, song.data)

    private fun notifyMediaStoreChanged() { App.instance.sendBroadcast(Intent(MusicService.MEDIA_STORE_CHANGED)) }

    companion object {
        private const val VERSION = 1

        private const val TABLE_NAME = "songs"

        const val COLUMNS_ID = "id" // long
        const val COLUMNS_PATH = "path" // string
        const val COLUMNS_TITLE = "title" // string
        const val COLUMNS_TIMESTAMP = "timestamp" // long

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
