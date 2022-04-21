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
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.AudioColumns
import player.phonograph.mediastore.SongLoader.getSongs
import player.phonograph.model.Song

/**
 * @author Andrew Neal, modified for Phonograph by Karim Abou Zeid
 *
 *
 * This keeps track of the music playback and history state of the playback service
 */
class MusicPlaybackQueueStore(context: Context?) : SQLiteOpenHelper(context, DatabaseConstants.MUSIC_PLAYBACK_STATE_DB, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        createTable(db, PLAYING_QUEUE_TABLE_NAME)
        createTable(db, ORIGINAL_PLAYING_QUEUE_TABLE_NAME)
    }

    private val mainTable: String = "(" +
        "${BaseColumns._ID} LONG NOT NULL," +
        "${AudioColumns.TITLE} TEXT NOT NULL," +
        "${AudioColumns.TRACK} INT NOT NULL," +
        "${AudioColumns.YEAR} INT NOT NULL," +
        "${AudioColumns.DURATION} LONG NOT NULL," +
        "${AudioColumns.DATA} TEXT NOT NULL," +
        "${AudioColumns.DATE_ADDED} LONG NOT NULL," +
        "${AudioColumns.DATE_MODIFIED} LONG NOT NULL," +
        "${AudioColumns.ALBUM_ID} LONG NOT NULL," +
        "${AudioColumns.ALBUM} TEXT NOT NULL," +
        "${AudioColumns.ARTIST_ID} LONG NOT NULL," +
        "${AudioColumns.ARTIST} TEXT NOT NULL" +
        ")"

    private fun createTable(db: SQLiteDatabase, tableName: String) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $tableName $mainTable;")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 4 && newVersion == 5) {
            migrate4to5(db)
        } else {
            // not necessary yet
            db.execSQL("DROP TABLE IF EXISTS $PLAYING_QUEUE_TABLE_NAME")
            db.execSQL("DROP TABLE IF EXISTS $ORIGINAL_PLAYING_QUEUE_TABLE_NAME")
            onCreate(db)
        }
    }

    private fun migrate4to5(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE $PLAYING_QUEUE_TABLE_NAME ADD ${AudioColumns.DATE_ADDED} LONG NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE $ORIGINAL_PLAYING_QUEUE_TABLE_NAME ADD ${AudioColumns.DATE_ADDED} LONG NOT NULL DEFAULT 0")
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
        val cursor = readableDatabase.query(
            tableName, null,
            null, null, null, null, null
        )
        return getSongs(cursor)
    }

    companion object {

        private var sInstance: MusicPlaybackQueueStore? = null

        const val PLAYING_QUEUE_TABLE_NAME = "playing_queue"
        const val ORIGINAL_PLAYING_QUEUE_TABLE_NAME = "original_playing_queue"
        private const val VERSION = 5

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
