/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.migration

import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.entity.FavoriteSongEntity
import player.phonograph.repo.room.entity.PinedPlaylistsEntity
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Migrate data, from legacy independent databases with SQLiteOpenHelper, to new integrated database with Androidx Room
 */
object GenesisMigrations {

    const val RESULT_OK = 0
    const val RESULT_NOT_FOUND = 1
    const val RESULT_ERROR = -1

    /**
     * Migrate legacy Favorites database data to new integrated database
     * @param db new database
     * @param path the legacy independent Favorites database path
     * @return migration result
     */
    suspend fun migrateFavoritesDatabase(
        context: Context,
        db: MusicDatabase,
        path: String?,
    ): Int {

        val oldFile: File? = if (path != null) File(path) else context.getDatabasePath(DATABASE_FAVORITE)
        if (oldFile == null || !oldFile.exists()) return RESULT_NOT_FOUND

        var legacyDb: SQLiteDatabase? = null
        return try {
            legacyDb = SQLiteDatabase.openDatabase(oldFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

            val projection = arrayOf(
                COLUMN_FAVORITE_ID,
                COLUMN_FAVORITE_PATH,
                COLUMN_FAVORITE_TITLE,
                COLUMN_FAVORITE_TIMESTAMP,
            )

            // Migrate songs
            legacyDb.query(TABLE_NAME_FAVORITE_SONGS, projection, null, null, null, null, null).use { cursor ->
                val songsDao = db.FavoritesSongsDao()
                while (cursor.moveToNext()) {
                    songsDao.add(
                        FavoriteSongEntity(
                            mediastoreId = cursor.getLong(0),
                            path = cursor.getString(1),
                            title = cursor.getString(2) ?: "",
                            date = cursor.getLong(3)
                        )
                    )
                }
            }

            legacyDb.query(TABLE_NAME_FAVORITE_PLAYLISTS, projection, null, null, null, null, null).use { cursor ->
                val playlistsDao = db.PinedPlaylistsDao()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val path = cursor.getString(1)
                    val title = cursor.getString(2) ?: ""
                    val timestamp = cursor.getLong(3)

                    playlistsDao.add(
                        PinedPlaylistsEntity(
                            id = id,
                            type = PinedPlaylistsEntity.TYPE_FILE_PLAYLIST,
                            sub = id,
                            data = path,
                            title = title,
                            date = timestamp
                        )
                    )
                }
            }

            RESULT_OK
        } catch (e: Exception) {
            e.printStackTrace()
            RESULT_ERROR
        } finally {
            legacyDb?.close()
        }
    }

    private const val DATABASE_FAVORITE = "favorite.db"

    private const val TABLE_NAME_FAVORITE_SONGS = "songs"
    private const val TABLE_NAME_FAVORITE_PLAYLISTS = "playlists"

    private const val COLUMN_FAVORITE_ID = "id" // long
    private const val COLUMN_FAVORITE_PATH = "path" // string
    private const val COLUMN_FAVORITE_TITLE = "title" // string
    private const val COLUMN_FAVORITE_TIMESTAMP = "timestamp" // long

    // private const val DATABASE_FAVORITE_DDL_SONGS =
    //     "CREATE TABLE IF NOT EXISTS $TABLE_NAME_FAVORITE_SONGS (" +
    //             "$COLUMNS_ID LONG NOT NULL PRIMARY KEY," +
    //             " $COLUMNS_PATH TEXT NOT NULL," +
    //             " $COLUMNS_TITLE TEXT," +
    //             " $COLUMNS_TIMESTAMP LONG);"
    //
    //
    // private const val DATABASE_FAVORITE_DDL_PLAYLISTS =
    //     "CREATE TABLE IF NOT EXISTS $TABLE_NAME_FAVORITE_PLAYLISTS (" +
    //             "$COLUMNS_ID LONG NOT NULL PRIMARY KEY," +
    //             " $COLUMNS_PATH TEXT NOT NULL," +
    //             " $COLUMNS_TITLE TEXT," +
    //             " $COLUMNS_TIMESTAMP LONG);"
}