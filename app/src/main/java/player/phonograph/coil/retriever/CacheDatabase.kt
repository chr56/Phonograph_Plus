/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.coil.retriever

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.database.sqlite.SQLiteOpenHelper

class CacheDatabase private constructor(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, VERSION) {


    override fun onCreate(db: SQLiteDatabase) {
        for (tableName in TABLE_NAMES_NON_EXISTENT) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS $tableName ($ID LONG NOT NULL, $TYPE INT NOT NULL, $TIMESTAMP LONG NOT NULL, PRIMARY KEY ($ID, $TYPE))"
            )
        }
        for (tableName in TABLE_NAMES_LOCATION) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS $tableName ($ID LONG NOT NULL, $TYPE INT NOT NULL, $TIMESTAMP LONG NOT NULL, $FILENAME TEXT NOT NULL, PRIMARY KEY ($ID, $TYPE))"
            )
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        removeAll(db)
        onCreate(db)
    }


    fun isNoImage(target: Target, id: Long, @RetrieverId retriever: Int): Boolean {
        val tableName = lookupNonExistentTableName(target)
        val result = readableDatabase.query(
            tableName,
            arrayOf(ID, TYPE, TIMESTAMP),
            "$ID = ? AND $TYPE = ?",
            arrayOf(id.toString(), retriever.toString()),
            null, null, TIMESTAMP
        ).use { cursor ->
            if (cursor.count > 0) {
                cursor.moveToFirst()
                // check timestamp
                val timestamp = cursor.getLong(2)
                val valid = (System.currentTimeMillis() - timestamp) < TIME_OUT
                if (!valid) removeNoImageMark(target, id, retriever)
                valid
            } else {
                false
            }
        }

        return result
    }

    fun isNoImage(target: Target, id: Long): Boolean {
        val tableName = lookupNonExistentTableName(target)
        val result = readableDatabase.query(
            tableName,
            arrayOf(ID, TYPE, TIMESTAMP),
            "$ID = ?",
            arrayOf(id.toString()),
            null, null, TIMESTAMP
        ).use { cursor ->
            cursor.count > 0
        }

        return result
    }

    private fun removeNoImageMark(target: Target, id: Long, @RetrieverId retriever: Int): Boolean {
        val tableName = lookupNonExistentTableName(target)
        return writableDatabase.delete(
            tableName, "$ID = ? AND $TYPE = ?",
            arrayOf(id.toString(), retriever.toString()),
        ).let { it > 0 }
    }

    fun markNoImage(target: Target, id: Long, @RetrieverId retriever: Int): Boolean {
        val tableName = lookupNonExistentTableName(target)
        return writableDatabase.insertWithOnConflict(
            tableName,
            null,
            ContentValues(3).apply {
                put(ID, id)
                put(TYPE, retriever)
                put(TIMESTAMP, System.currentTimeMillis())
            },
            CONFLICT_REPLACE
        ).let { it > 0 }
    }


    fun fetchLocation(target: Target, id: Long, @RetrieverId retriever: Int): String? {
        val tableName = lookupLocationTableName(target)
        return readableDatabase.query(
            tableName,
            arrayOf(ID, TYPE, TIMESTAMP, FILENAME),
            "$ID = ? AND $TYPE = ?",
            arrayOf(id.toString(), retriever.toString()),
            null, null, TIMESTAMP
        ).use { cursor ->
            if (cursor.count > 0) {
                cursor.moveToFirst()
                // check timestamp
                val timestamp = cursor.getLong(2)
                val valid = (System.currentTimeMillis() - timestamp) < TIME_OUT
                cursor.getString(3)
            } else {
                null
            }
        }
    }

    fun storeLocation(target: Target, id: Long, @RetrieverId retriever: Int, filename: String): Boolean {
        val tableName = lookupLocationTableName(target)
        return writableDatabase.insertWithOnConflict(
            tableName,
            null,
            ContentValues(4).apply {
                put(ID, id)
                put(TYPE, retriever)
                put(TIMESTAMP, System.currentTimeMillis())
                put(FILENAME, filename)
            },
            CONFLICT_REPLACE
        ).let { it > 0 }
    }


    fun clear() = removeAll(writableDatabase)

    private fun removeAll(db: SQLiteDatabase) {
        for (tableName in TABLE_NAMES_NON_EXISTENT) {
            db.execSQL(
                "DROP TABLE IF EXISTS $tableName"
            )
        }
        for (tableName in TABLE_NAMES_LOCATION) {
            db.execSQL(
                "DROP TABLE IF EXISTS $tableName"
            )
        }
    }

    private fun lookupNonExistentTableName(target: Target): String = when (target) {
        Target.SONG   -> TABLE_NAME_SONGS_NON_EXISTENT
        Target.ALBUM  -> TABLE_NAME_ALBUMS_NON_EXISTENT
        Target.ARTIST -> TABLE_NAME_ARTISTS_NON_EXISTENT
    }

    private fun lookupLocationTableName(target: Target): String = when (target) {
        Target.SONG   -> TABLE_NAME_SONGS_LOCATION
        Target.ALBUM  -> TABLE_NAME_ALBUMS_LOCATION
        Target.ARTIST -> TABLE_NAME_ARTISTS_LOCATION
    }

    fun release(): Boolean {
        return synchronized(this) {
            refCount -= 1
            if (refCount <= 0) {
                close()
                true
            } else {
                false
            }
        }
    }

    companion object {
        const val VERSION = 1
        const val DB_NAME = "_image_cache.db"

        private const val TABLE_NAME_SONGS_NON_EXISTENT = "songs_non_existent"
        private const val TABLE_NAME_ALBUMS_NON_EXISTENT = "albums_non_existent"
        private const val TABLE_NAME_ARTISTS_NON_EXISTENT = "artists_non_existent"


        private const val TABLE_NAME_SONGS_LOCATION = "songs_location"
        private const val TABLE_NAME_ALBUMS_LOCATION = "albums_location"
        private const val TABLE_NAME_ARTISTS_LOCATION = "artists_location"

        private val TABLE_NAMES_NON_EXISTENT: Array<String> = arrayOf(
            TABLE_NAME_SONGS_NON_EXISTENT,
            TABLE_NAME_ALBUMS_NON_EXISTENT,
            TABLE_NAME_ARTISTS_NON_EXISTENT,
        )
        private val TABLE_NAMES_LOCATION: Array<String> = arrayOf(
            TABLE_NAME_SONGS_LOCATION,
            TABLE_NAME_ALBUMS_LOCATION,
            TABLE_NAME_ARTISTS_LOCATION,
        )

        private const val ID = "id"
        private const val TYPE = "type"
        private const val TIMESTAMP = "timestamp"
        private const val FILENAME = "filename"


        private const val TIME_OUT: Long = 7 * (24 * 60 * 60 * 1000)


        private var sInstance: CacheDatabase? = null
        private var refCount = 0
        fun instance(context: Context): CacheDatabase {
            val cacheDatabase = sInstance
            return synchronized(this) {
                if (cacheDatabase != null) {
                    refCount += 1
                    cacheDatabase
                } else {
                    sInstance = CacheDatabase(context)
                    refCount = 1
                    sInstance!!
                }
            }
        }
    }

    enum class Target {
        SONG, ALBUM, ARTIST
    }
}