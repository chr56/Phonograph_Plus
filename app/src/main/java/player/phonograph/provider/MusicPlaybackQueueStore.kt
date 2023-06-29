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

import player.phonograph.mediastore.internal.intoSongs
import player.phonograph.model.Song
import player.phonograph.util.reportError
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.AudioColumns

/**
 * @author Andrew Neal, modified for Phonograph by Karim Abou Zeid
 *
 *
 * This keeps track of the music playback and history state of the playback service
 */
class MusicPlaybackQueueStore(context: Context?) : SQLiteOpenHelper(
    context, DatabaseConstants.MUSIC_PLAYBACK_STATE_DB, null, VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        createTable(db, PLAYING_QUEUE_TABLE_NAME)
        createTable(db, ORIGINAL_PLAYING_QUEUE_TABLE_NAME)
    }

    private val mainTable: String =
        "(" + "${BaseColumns._ID} LONG NOT NULL," + "${AudioColumns.TITLE} TEXT NOT NULL," + "${AudioColumns.TRACK} INT NOT NULL," + "${AudioColumns.YEAR} INT NOT NULL," + "${AudioColumns.DURATION} LONG NOT NULL," + "${AudioColumns.DATA} TEXT NOT NULL," + "${AudioColumns.DATE_ADDED} LONG NOT NULL," + "${AudioColumns.DATE_MODIFIED} LONG NOT NULL," + "${AudioColumns.ALBUM_ID} LONG NOT NULL," + "${AudioColumns.ALBUM} TEXT NOT NULL," + "${AudioColumns.ARTIST_ID} LONG NOT NULL," + "${AudioColumns.ARTIST} TEXT NOT NULL," + "${AudioColumns.ALBUM_ARTIST} TEXT," + "${AudioColumns.COMPOSER} TEXT" + ")"

    private fun createTable(db: SQLiteDatabase, tableName: String) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $tableName $mainTable;")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 5 && newVersion == 6) {
            migrate5to6(db)
        } else if (oldVersion == 4 && newVersion == 6) {
            migrate4to5(db)
            migrate5to6(db)
        } else {
            // not necessary yet
            db.execSQL("DROP TABLE IF EXISTS $PLAYING_QUEUE_TABLE_NAME")
            db.execSQL("DROP TABLE IF EXISTS $ORIGINAL_PLAYING_QUEUE_TABLE_NAME")
            onCreate(db)
        }
    }

    private fun migrate4to5(db: SQLiteDatabase) {
        alterTable4to5(db, PLAYING_QUEUE_TABLE_NAME)
        alterTable4to5(db, ORIGINAL_PLAYING_QUEUE_TABLE_NAME)
    }

    private fun alterTable4to5(db: SQLiteDatabase, tableName: String) {
        db.beginTransaction()
        try {
            val temp = tableName + "_new"
            // Thread.sleep(2_000)
            db.execSQL("DROP TABLE IF EXISTS $temp")
            db.execSQL(
                "CREATE TABLE $temp" +
                        "(" +
                        "${BaseColumns._ID} LONG NOT NULL," +
                        "${AudioColumns.TITLE} TEXT NOT NULL," +
                        "${AudioColumns.TRACK} INT NOT NULL," +
                        "${AudioColumns.YEAR} INT NOT NULL," +
                        "${AudioColumns.DURATION} LONG NOT NULL," +
                        "${AudioColumns.DATA} TEXT NOT NULL," +
                        "${AudioColumns.DATE_ADDED} LONG NOT NULL DEFAULT 0," +
                        "${AudioColumns.DATE_MODIFIED} LONG NOT NULL," +
                        "${AudioColumns.ALBUM_ID} LONG NOT NULL," +
                        "${AudioColumns.ALBUM} TEXT NOT NULL," +
                        "${AudioColumns.ARTIST_ID} LONG NOT NULL," +
                        "${AudioColumns.ARTIST} TEXT NOT NULL" +
                        ");"
            )
            db.execSQL(
                "INSERT INTO $temp(${BaseColumns._ID},${AudioColumns.TITLE},${AudioColumns.TRACK},${AudioColumns.YEAR},${AudioColumns.DURATION},${AudioColumns.DATA},${AudioColumns.DATE_MODIFIED},${AudioColumns.ALBUM_ID},${AudioColumns.ALBUM},${AudioColumns.ARTIST_ID},${AudioColumns.ARTIST}) " +
                        "SELECT ${BaseColumns._ID},${AudioColumns.TITLE},${AudioColumns.TRACK},${AudioColumns.YEAR},${AudioColumns.DURATION},${AudioColumns.DATA},${AudioColumns.DATE_MODIFIED},${AudioColumns.ALBUM_ID},${AudioColumns.ALBUM},${AudioColumns.ARTIST_ID},${AudioColumns.ARTIST}" +
                        " FROM $tableName"
            )
            db.execSQL("DROP TABLE IF EXISTS $tableName ")
            db.execSQL("ALTER TABLE $temp RENAME TO $tableName")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            reportError(
                e,
                "MusicPlaybackQueueStore",
                "Fail to transaction playback database, playing queue now corrupted!"
            )

            try {
                // fallback
                db.execSQL("ALTER TABLE $tableName ADD ${AudioColumns.DATE_ADDED} LONG NOT NULL DEFAULT 0")
            } catch (e: Exception) {
            } finally {
                db.setTransactionSuccessful()
            }

            throw e
        } finally {
            db.endTransaction()
        }
    }


    private fun migrate5to6(db: SQLiteDatabase) {
        alterTable5to6(db, PLAYING_QUEUE_TABLE_NAME)
        alterTable5to6(db, ORIGINAL_PLAYING_QUEUE_TABLE_NAME)
    }

    private fun alterTable5to6(db: SQLiteDatabase, tableName: String) {
        db.beginTransaction()
        try {
            val temp = tableName + "_new"
            db.execSQL("DROP TABLE IF EXISTS $temp")
            db.execSQL(
                "CREATE TABLE $temp" + "(" + "${BaseColumns._ID} LONG NOT NULL," + "${AudioColumns.TITLE} TEXT NOT NULL," + "${AudioColumns.TRACK} INT NOT NULL," + "${AudioColumns.YEAR} INT NOT NULL," + "${AudioColumns.DURATION} LONG NOT NULL," + "${AudioColumns.DATA} TEXT NOT NULL," + "${AudioColumns.DATE_ADDED} LONG NOT NULL," + "${AudioColumns.DATE_MODIFIED} LONG NOT NULL," + "${AudioColumns.ALBUM_ID} LONG NOT NULL," + "${AudioColumns.ALBUM} TEXT NOT NULL," + "${AudioColumns.ARTIST_ID} LONG NOT NULL," + "${AudioColumns.ARTIST} TEXT NOT NULL," + "${AudioColumns.ALBUM_ARTIST} TEXT," + "${AudioColumns.COMPOSER} TEXT" + ");"
            )
            db.execSQL(
                "INSERT INTO $temp(${BaseColumns._ID},${AudioColumns.TITLE},${AudioColumns.TRACK},${AudioColumns.YEAR},${AudioColumns.DURATION},${AudioColumns.DATA},${AudioColumns.DATE_ADDED},${AudioColumns.DATE_MODIFIED},${AudioColumns.ALBUM_ID},${AudioColumns.ALBUM},${AudioColumns.ARTIST_ID},${AudioColumns.ARTIST},${AudioColumns.ALBUM_ARTIST},${AudioColumns.COMPOSER}) " + "SELECT ${BaseColumns._ID},${AudioColumns.TITLE},${AudioColumns.TRACK},${AudioColumns.YEAR},${AudioColumns.DURATION},${AudioColumns.DATA},${AudioColumns.DATE_ADDED},${AudioColumns.DATE_MODIFIED},${AudioColumns.ALBUM_ID},${AudioColumns.ALBUM},${AudioColumns.ARTIST_ID},${AudioColumns.ARTIST}, NULL, NULL" + " FROM $tableName"
            )
            db.execSQL("DROP TABLE IF EXISTS $tableName ")
            db.execSQL("ALTER TABLE $temp RENAME TO $tableName")
            db.setTransactionSuccessful()
        } catch (e: Exception) {

            reportError(
                e,
                "MusicPlaybackQueueStore",
                "Fail to transaction playback database, playing queue now corrupted!"
            )

            try {
                // fallback
                db.execSQL("ALTER TABLE $tableName ADD ${AudioColumns.ALBUM_ARTIST} TEXT")
                db.execSQL("ALTER TABLE $tableName ADD ${AudioColumns.COMPOSER} TEXT")
            } catch (e: Exception) {
            } finally {
                db.setTransactionSuccessful()
            }


            throw e
        } finally {
            db.endTransaction()
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // If we ever have downgrade, drop the table to be safe
        db.execSQL("DROP TABLE IF EXISTS $PLAYING_QUEUE_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $ORIGINAL_PLAYING_QUEUE_TABLE_NAME")
        onCreate(db)
    }

    @Synchronized
    fun saveQueues(playingQueue: List<Song>, originalPlayingQueue: List<Song>) {
        saveQueue(PLAYING_QUEUE_TABLE_NAME, playingQueue)
        saveQueue(ORIGINAL_PLAYING_QUEUE_TABLE_NAME, originalPlayingQueue)
    }

    /**
     * Clears the existing database and saves the queue into the db so that when the
     * app is restarted, the tracks you were listening to is restored
     *
     * @param queue the queue to save
     */
    @Synchronized
    private fun saveQueue(tableName: String, queue: List<Song>) {
        val database = writableDatabase
        database.beginTransaction()
        try {
            database.delete(tableName, null, null)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
        val NUM_PROCESS = 20
        var position = 0
        while (position < queue.size) {
            database.beginTransaction()
            try {
                var i = position
                while (i < queue.size && i < position + NUM_PROCESS) {
                    val song = queue[i]
                    val values = ContentValues(4)
                    values.put(BaseColumns._ID, song.id)
                    values.put(AudioColumns.TITLE, song.title)
                    values.put(AudioColumns.TRACK, song.trackNumber)
                    values.put(AudioColumns.YEAR, song.year)
                    values.put(AudioColumns.DURATION, song.duration)
                    values.put(AudioColumns.DATA, song.data)
                    values.put(AudioColumns.DATE_ADDED, song.dateAdded)
                    values.put(AudioColumns.DATE_MODIFIED, song.dateModified)
                    values.put(AudioColumns.ALBUM_ID, song.albumId)
                    values.put(AudioColumns.ALBUM, song.albumName)
                    values.put(AudioColumns.ARTIST_ID, song.artistId)
                    values.put(AudioColumns.ARTIST, song.artistName)
                    values.put(AudioColumns.ALBUM_ARTIST, song.albumArtistName)
                    values.put(AudioColumns.COMPOSER, song.composer)
                    database.insert(tableName, null, values)
                    i++
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
                position += NUM_PROCESS
            }
        }
    }

    val savedPlayingQueue: List<Song>
        get() = getQueue(PLAYING_QUEUE_TABLE_NAME)
    val savedOriginalPlayingQueue: List<Song>
        get() = getQueue(ORIGINAL_PLAYING_QUEUE_TABLE_NAME)

    private fun getQueue(tableName: String): List<Song> {
        return readableDatabase.query(
            tableName, null,
            null, null, null, null, null
        ).use { cursor -> cursor.intoSongs() }
    }

    companion object {

        private var sInstance: MusicPlaybackQueueStore? = null

        const val PLAYING_QUEUE_TABLE_NAME = "playing_queue"
        const val ORIGINAL_PLAYING_QUEUE_TABLE_NAME = "original_playing_queue"
        private const val VERSION = 6

        /**
         * @param context The [Context] to use
         * @return A new instance of this class.
         */
        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): MusicPlaybackQueueStore {
            if (sInstance == null) {
                sInstance = MusicPlaybackQueueStore(context.applicationContext)
            }
            return sInstance!!
        }
    }
}
