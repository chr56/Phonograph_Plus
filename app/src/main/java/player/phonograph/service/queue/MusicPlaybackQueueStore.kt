/*
 *  Copyright (c) 2022~2024 chr_56
 */
package player.phonograph.service.queue

import player.phonograph.foundation.warning
import player.phonograph.model.Song
import player.phonograph.repo.database.DatabaseConstants
import player.phonograph.repo.mediastore.internal.intoSongs
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

    private fun createTable(db: SQLiteDatabase, tableName: String) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $tableName $TABLE;")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        warning(
            "MusicPlaybackQueueStore",
            "Upgrade from an already unsupported version ($oldVersion) to the version ($newVersion) , Playing Queue cleaned"
        )
        db.execSQL("DROP TABLE IF EXISTS $PLAYING_QUEUE_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $ORIGINAL_PLAYING_QUEUE_TABLE_NAME")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        warning(
            "MusicPlaybackQueueStore",
            "Downgrade from the version ($newVersion) to an unsupported version ($oldVersion) , Playing Queue cleaned"
        )
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
        val concurrent = 20
        var position = 0
        while (position < queue.size) {
            database.beginTransaction()
            try {
                var i = position
                while (i < queue.size && i < position + concurrent) {
                    val song = queue[i]
                    val values = ContentValues(14).apply {
                        put(BaseColumns._ID, song.id)
                        put(AudioColumns.TITLE, song.title)
                        put(AudioColumns.TRACK, song.trackNumber)
                        put(AudioColumns.YEAR, song.year)
                        put(AudioColumns.DURATION, song.duration)
                        put(AudioColumns.DATA, song.data)
                        put(AudioColumns.DATE_ADDED, song.dateAdded)
                        put(AudioColumns.DATE_MODIFIED, song.dateModified)
                        put(AudioColumns.ALBUM_ID, song.albumId)
                        put(AudioColumns.ALBUM, song.albumName)
                        put(AudioColumns.ARTIST_ID, song.artistId)
                        put(AudioColumns.ARTIST, song.artistName)
                        put(AudioColumns.ALBUM_ARTIST, song.albumArtistName)
                        put(AudioColumns.COMPOSER, song.composer)
                    }
                    database.insert(tableName, null, values)
                    i++
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
                position += concurrent
            }
        }
    }

    val savedPlayingQueue: List<Song> get() = getQueue(PLAYING_QUEUE_TABLE_NAME)
    val savedOriginalPlayingQueue: List<Song> get() = getQueue(ORIGINAL_PLAYING_QUEUE_TABLE_NAME)

    private fun getQueue(tableName: String): List<Song> {
        return readableDatabase.query(
            tableName, null,
            null, null, null, null, null
        ).use { cursor -> cursor.intoSongs() }
    }

    companion object {

        const val PLAYING_QUEUE_TABLE_NAME = "playing_queue"
        const val ORIGINAL_PLAYING_QUEUE_TABLE_NAME = "original_playing_queue"


        private const val TABLE: String = "(" +
                "${BaseColumns._ID} LONG NOT NULL, " +
                "${AudioColumns.TITLE} TEXT NOT NULL, " +
                "${AudioColumns.TRACK} INT NOT NULL, " +
                "${AudioColumns.YEAR} INT NOT NULL, " +
                "${AudioColumns.DURATION} LONG NOT NULL, " +
                "${AudioColumns.DATA} TEXT NOT NULL, " +
                "${AudioColumns.DATE_ADDED} LONG NOT NULL, " +
                "${AudioColumns.DATE_MODIFIED} LONG NOT NULL, " +
                "${AudioColumns.ALBUM_ID} LONG NOT NULL, " +
                "${AudioColumns.ALBUM} TEXT NOT NULL, " +
                "${AudioColumns.ARTIST_ID} LONG NOT NULL, " +
                "${AudioColumns.ARTIST} TEXT NOT NULL, " +
                "${AudioColumns.ALBUM_ARTIST} TEXT, " +
                "${AudioColumns.COMPOSER} TEXT" +
                ")"


        private const val VERSION = 6

    }
}
