/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.repo.sync.ProgressConnection
import player.phonograph.model.repo.sync.SyncReport
import player.phonograph.repo.room.sync.SyncExecutors
import androidx.room.withTransaction
import android.content.Context
import android.util.Log

object DatabaseActions {

    private suspend fun deleteTablesOfNonUserData(musicDatabase: MusicDatabase) {
        musicDatabase.RelationshipManipulateDao().deleteAllGenreSongs()
        musicDatabase.RelationshipManipulateDao().deleteAllAlbumArtists()
        musicDatabase.RelationshipManipulateDao().deleteAllArtistSongs()
        musicDatabase.AlbumManipulateDao().deleteAll()
        musicDatabase.ArtistManipulateDao().deleteAll()
        musicDatabase.SongManipulateDao().deleteAll()
        musicDatabase.GenreManipulateDao().deleteAll()
    }

    /**
     * Close Database and delete database file
     *
     * **Require reopen or reboot after operation**
     *
     * @return deletion result
     */
    fun purge(context: Context, musicDatabase: MusicDatabase): Boolean {
        val path = context.getDatabasePath(MusicDatabase.DATABASE_NAME)
        return synchronized(musicDatabase) {
            musicDatabase.close()
            path.delete()
        }
    }


    /**
     * Sync database:
     * check MediaStore, refresh database if it has changes
     * @param progress message callback
     * @param force refresh without check
     */
    suspend fun sync(
        context: Context,
        musicDatabase: MusicDatabase,
        progress: ProgressConnection? = null,
        force: Boolean = false,
    ): SyncReport? {
        val syncExecutor = SyncExecutors.obtain(context, musicDatabase)
        return if (force || syncExecutor.check(context)) {
            try {
                syncExecutor.sync(context, progress)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync database.", e)
                null
            } finally {
                EventHub.sendEvent(context.applicationContext, EventHub.EVENT_MUSIC_LIBRARY_CHANGED)
            }
        } else {
            null
        }

    }

    /**
     * Wipe tables from database:
     * Delete data
     *
     * @param includeUserData true if wipe all tables
     */
    suspend fun wipe(
        context: Context,
        musicDatabase: MusicDatabase,
        progress: ProgressConnection? = null,
        includeUserData: Boolean = false,
    ): Boolean {
        return try {
            if (includeUserData) {
                progress?.onProcessUpdate("Wipe database...")
                musicDatabase.clearAllTables()
            } else {
                progress?.onProcessUpdate("Clear database...")
                musicDatabase.withTransaction {
                    deleteTablesOfNonUserData(musicDatabase)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wipe database.", e)
            false
        } finally {
            EventHub.sendEvent(context.applicationContext, EventHub.EVENT_MUSIC_LIBRARY_CHANGED)
        }
    }


    /**
     * Rebuild database:
     * clear cached or devived tables from database, and reimport them again
     */
    suspend fun rebuild(
        context: Context,
        musicDatabase: MusicDatabase,
        progress: ProgressConnection? = null,
    ): SyncReport? {
        progress?.onStart(7328453)
        val wipeResult = wipe(context, musicDatabase, progress, includeUserData = false)
        val syncResult = sync(context, musicDatabase, progress, force = !wipeResult)
        progress?.onCompleted()
        return syncResult
    }

    private const val TAG = "Database"
}