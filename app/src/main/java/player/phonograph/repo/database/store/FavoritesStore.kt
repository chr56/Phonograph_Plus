/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.database.store

import org.koin.core.context.GlobalContext
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.Song
import player.phonograph.model.playlist.DatabasePlaylistLocation
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.database.DatabaseConstants.FAVORITE_DB
import player.phonograph.util.text.currentTimestamp
import player.phonograph.util.warning
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FavoritesStore(context: Context) :
        SQLiteOpenHelper(context, FAVORITE_DB, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(creatingSongsTableSQL)
        db.execSQL(creatingPlaylistsTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        warning(FAVORITE_DB, "Can not upgrade database `favorite.db` from $oldVersion to $newVersion ")
        onCreate(db)
    }

    fun clearAllSongs() = clearTable(TABLE_NAME_SONGS)
    fun clearAllPlaylists() = clearTable(TABLE_NAME_PLAYLISTS)

    private fun clearTable(tableName: String) {
        writableDatabase.delete(tableName, null, null)
        mediaStoreTracker.notifyAllListeners()
    }

    suspend fun getAllSongs(parser: suspend (Long, String, String, Long) -> Song?): List<Song> =
        parseCursorImpl(TABLE_NAME_SONGS) { cursor ->
            parser(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getLong(3),
            )
        }

    suspend fun getAllPlaylists(parser: suspend (Long, String, String, Long) -> Playlist?): List<Playlist> =
        parseCursorImpl(TABLE_NAME_PLAYLISTS) { cursor ->
            parser(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getLong(3),
            )
        }

    private suspend fun <T> parseCursorImpl(tableName: String, operation: suspend (Cursor) -> T?): List<T> {
        return query(tableName).use { cursor ->
            val notEmpty = cursor.moveToFirst()
            if (notEmpty) {
                val result = mutableListOf<T>()
                do {
                    val item = operation(cursor)
                    if (item != null) result.add(item)
                } while (cursor.moveToNext())
                result
            } else {
                emptyList()
            }
        }
    }

    private fun query(tableName: String): Cursor {
        val database = readableDatabase
        return database.query(
            tableName,
            arrayOf(COLUMNS_ID, COLUMNS_PATH, COLUMNS_TITLE, COLUMNS_TIMESTAMP),
            null, null, null, null, "$COLUMNS_TIMESTAMP DESC"
        )
    }



    fun containsSong(songId: Long?, path: String?): Boolean =
        containsImpl(TABLE_NAME_SONGS, songId, path)

    fun containsPlaylist(playlist: Playlist): Boolean =
        if (!playlist.isVirtual()) containsImpl(TABLE_NAME_PLAYLISTS, playlist.mediaStoreId(), playlist.path())
        else false

    private fun containsImpl(table: String, id: Long?, path: String?): Boolean {
        val database = readableDatabase
        val cursor = database.query(
            table,
            arrayOf(COLUMNS_ID, COLUMNS_PATH, COLUMNS_TITLE, COLUMNS_TIMESTAMP),
            "$COLUMNS_ID =? OR $COLUMNS_PATH =?",
            arrayOf(id?.toString() ?: "0", path ?: ""),
            null, null, null,
        )
        return cursor.use { it.moveToFirst() }
    }

    fun addSong(song: Song): Boolean =
        addImpl(TABLE_NAME_SONGS, song.id, song.data, song.title)

    fun addPlaylist(playlist: Playlist): Boolean =
        when (val location = playlist.location) {
            is DatabasePlaylistLocation -> addImpl(
                TABLE_NAME_PLAYLISTS,
                location.id(),
                location.databaseId.toString(),
                playlist.name
            )

            is FilePlaylistLocation     -> addImpl(
                TABLE_NAME_PLAYLISTS,
                location.mediastoreId,
                location.path,
                playlist.name
            )

            else                        -> false // unsupported
        }

    private fun addImpl(tableName: String, id: Long, path: String, name: String?): Boolean {
        val database = writableDatabase
        database.beginTransaction()
        return try {
            val values = ContentValues(4)
                .apply {
                    put(COLUMNS_ID, id)
                    put(COLUMNS_PATH, path)
                    put(COLUMNS_TITLE, name)
                    put(COLUMNS_TIMESTAMP, currentTimestamp())
                }
            database.insert(tableName, null, values)
            database.setTransactionSuccessful()
            true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        } finally {
            database.endTransaction()
            mediaStoreTracker.notifyAllListeners()
        }
    }

    fun addSongs(songs: Collection<Song>): Boolean {
        val data = songs.map {
            ContentValues(4).apply {
                put(COLUMNS_ID, it.id)
                put(COLUMNS_PATH, it.data)
                put(COLUMNS_TITLE, it.title)
                put(COLUMNS_TIMESTAMP, currentTimestamp())
            }
        }
        return addMultipleImpl(TABLE_NAME_SONGS, data)
    }

    fun addPlaylists(playlists: Collection<Playlist>): Boolean {
        val data = playlists.filter { !it.isVirtual() }.map { playlist ->
            ContentValues(4).apply {
                when (val location = playlist.location) {
                    is FilePlaylistLocation     -> {
                        put(COLUMNS_ID, location.mediastoreId)
                        put(COLUMNS_PATH, location.path)
                        put(COLUMNS_TITLE, playlist.name)
                        put(COLUMNS_TIMESTAMP, currentTimestamp())
                    }

                    is DatabasePlaylistLocation -> {
                        put(COLUMNS_ID, location.id())
                        put(COLUMNS_PATH, location.databaseId.toString())
                        put(COLUMNS_TITLE, playlist.name)
                        put(COLUMNS_TIMESTAMP, currentTimestamp())
                    }

                    else                        -> {}
                }
            }
        }
        return addMultipleImpl(TABLE_NAME_PLAYLISTS, data)
    }

    private fun addMultipleImpl(tableName: String, lines: List<ContentValues>): Boolean {
        val database = writableDatabase
        database.beginTransaction()
        return try {
            for (line in lines) {
                database.insert(tableName, null, line)
            }
            database.setTransactionSuccessful()
            true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        } finally {
            database.endTransaction()
            mediaStoreTracker.notifyAllListeners()
        }
    }

    fun removeSong(song: Song): Boolean =
        removeImpl(TABLE_NAME_SONGS, song.id, song.data)

    fun removePlaylist(playlist: Playlist): Boolean =
        when (val location = playlist.location) {
            is DatabasePlaylistLocation -> removeImpl(
                TABLE_NAME_PLAYLISTS,
                location.id(),
                location.databaseId.toString(),
            )

            is FilePlaylistLocation     -> removeImpl(
                TABLE_NAME_PLAYLISTS,
                location.mediastoreId,
                location.path,
            )

            else                        -> false // unsupported
        }

    private fun removeImpl(table: String, id: Long, path: String): Boolean {
        val database = writableDatabase
        database.beginTransaction()
        return try {
            val result = database.delete(
                table,
                "$COLUMNS_ID =? AND $COLUMNS_PATH =?",
                arrayOf(id.toString(), path)
            )
            database.setTransactionSuccessful()
            result > 0
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        } finally {
            database.endTransaction()
            mediaStoreTracker.notifyAllListeners()
        }
    }

    /**
     * cleaning mission songs
     * @param checker check id and path, return true if invalid
     */
    suspend fun cleanMissingSongs(checker: suspend (Long, String) -> Boolean): Boolean {
        val paths: List<Pair<Long, String>> =
            query(TABLE_NAME_SONGS).use { cursor ->
                if (cursor.moveToFirst()) {
                    val paths = mutableListOf<Pair<Long, String>>()
                    do {
                        try {
                            val id = cursor.getString(0).toLong()
                            val path = cursor.getString(1)
                            val result = checker(id, path)
                            if (result) {
                                paths.add(id to path)
                            }
                        } catch (_: Exception) {
                        }
                    } while (cursor.moveToNext())
                    paths
                } else {
                    emptyList()
                }
            }
        for ((id, path) in paths) {
            removeImpl(TABLE_NAME_SONGS, id, path)
        }
        return paths.isNotEmpty()
    }

    private val mediaStoreTracker: MediaStoreTracker by GlobalContext.get().inject()

    companion object {
        private const val VERSION = 2

        private const val TABLE_NAME_SONGS = "songs"
        private const val TABLE_NAME_PLAYLISTS = "playlists"

        const val COLUMNS_ID = "id" // long
        const val COLUMNS_PATH = "path" // string
        const val COLUMNS_TITLE = "title" // string
        const val COLUMNS_TIMESTAMP = "timestamp" // long


        private const val creatingSongsTableSQL =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_SONGS (" +
                    "$COLUMNS_ID LONG NOT NULL PRIMARY KEY," +
                    " $COLUMNS_PATH TEXT NOT NULL," +
                    " $COLUMNS_TITLE TEXT," +
                    " $COLUMNS_TIMESTAMP LONG);"


        private const val creatingPlaylistsTableSQL =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_PLAYLISTS (" +
                    "$COLUMNS_ID LONG NOT NULL PRIMARY KEY," +
                    " $COLUMNS_PATH TEXT NOT NULL," +
                    " $COLUMNS_TITLE TEXT," +
                    " $COLUMNS_TIMESTAMP LONG);"


        fun get() = GlobalContext.get().get<FavoritesStore>()
    }
}