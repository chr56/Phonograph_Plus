/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil.cache

import player.phonograph.coil.retriever.ImageRetrievers.RetrieverId
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.database.sqlite.SQLiteOpenHelper

class CacheDatabase private constructor(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, VERSION) {


    override fun onCreate(db: SQLiteDatabase) {
        for (tableName in TABLE_NAMES) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS $tableName ($ID LONG NOT NULL, $TYPE INT NOT NULL, $TIMESTAMP LONG NOT NULL, $EMPTY BOOLEAN NOT NULL,$FILENAME TEXT, PRIMARY KEY ($ID, $TYPE))"
            )
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        removeAll(db)
        onCreate(db)
    }


    fun fetch(target: Target, id: Long, @RetrieverId retriever: Int): FetchedCache {
        val tableName = lookupTableName(target)
        return readableDatabase.query(
            tableName,
            arrayOf(ID, TYPE, TIMESTAMP, EMPTY, FILENAME),
            "$ID = ? AND $TYPE = ?",
            arrayOf(id.toString(), retriever.toString()),
            null, null, TIMESTAMP
        ).use { cursor ->
            if (cursor.count > 0) {
                cursor.moveToFirst()

                // check timestamp
                val timestamp = cursor.getLong(2)
                val valid = (System.currentTimeMillis() - timestamp) < TIME_OUT
                if (!valid) remove(target, id, retriever)

                // check empty
                val empty = cursor.getInt(3)
                if (empty == 0) {
                    FetchedCache.Empty
                } else {
                    FetchedCache.Existed(cursor.getString(4))
                }
            } else {
                FetchedCache.Unknown
            }
        }
    }

    fun remove(target: Target, id: Long, @RetrieverId retriever: Int): Boolean {
        val tableName = lookupTableName(target)
        return writableDatabase.delete(
            tableName, "$ID = ? AND $TYPE = ?",
            arrayOf(id.toString(), retriever.toString()),
        ).let { it > 0 }
    }

    /**
     * @param filename null if mark empty
     */
    fun register(target: Target, id: Long, @RetrieverId retriever: Int, filename: String?): Boolean {
        val tableName = lookupTableName(target)
        val contentValues = if (filename != null) {
            ContentValues(5).apply {
                put(ID, id)
                put(TYPE, retriever)
                put(TIMESTAMP, System.currentTimeMillis())
                put(EMPTY, false)
                put(FILENAME, filename)
            }
        } else {
            ContentValues(5).apply {
                put(ID, id)
                put(TYPE, retriever)
                put(TIMESTAMP, System.currentTimeMillis())
                put(EMPTY, true)
            }
        }
        return writableDatabase.insertWithOnConflict(
            tableName,
            null,
            contentValues,
            CONFLICT_REPLACE
        ).let { it > 0 }
    }


    fun clear() = removeAll(writableDatabase)

    private fun removeAll(db: SQLiteDatabase) {
        for (tableName in TABLE_NAMES) {
            db.execSQL(
                "DELETE FROM $tableName"
            )
        }

    }

    private fun lookupTableName(target: Target): String = when (target) {
        Target.SONG   -> TABLE_NAME_SONGS
        Target.ALBUM  -> TABLE_NAME_ALBUMS
        Target.ARTIST -> TABLE_NAME_ARTISTS
    }

    companion object {
        const val VERSION = 2
        const val DB_NAME = "_image_cache.db"

        private const val TABLE_NAME_SONGS = "songs"
        private const val TABLE_NAME_ALBUMS = "albums"
        private const val TABLE_NAME_ARTISTS = "artists"

        private val TABLE_NAMES: Array<String> = arrayOf(
            TABLE_NAME_SONGS,
            TABLE_NAME_ALBUMS,
            TABLE_NAME_ARTISTS,
        )
        private const val ID = "id"
        private const val TYPE = "type"
        private const val TIMESTAMP = "timestamp"
        private const val EMPTY = "empty"
        private const val FILENAME = "filename"


        private const val TIME_OUT: Long = 7 * (24 * 60 * 60 * 1000)


        private var sInstance: CacheDatabase? = null
        fun instance(context: Context): CacheDatabase {
            val cacheDatabase = sInstance
            return synchronized(this) {
                if (cacheDatabase != null) {
                    cacheDatabase
                } else {
                    sInstance = CacheDatabase(context)
                    sInstance!!
                }
            }
        }
    }

    enum class Target {
        SONG, ALBUM, ARTIST
    }

    sealed interface FetchedCache {
        data object Empty : FetchedCache
        data object Unknown : FetchedCache
        class Existed(val path: String) : FetchedCache

        fun isEmpty(): Boolean = this is Empty

        fun existedOrNull(): String? = if (this is Existed) path else null
    }
}