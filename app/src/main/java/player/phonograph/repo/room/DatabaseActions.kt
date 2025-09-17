/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.foundation.notification.ProgressNotificationConnection
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.repo.sync.SyncReport
import player.phonograph.model.repo.PROVIDER_MEDIASTORE_MIRROR
import player.phonograph.model.repo.PROVIDER_MEDIASTORE_PARSED
import player.phonograph.model.repo.sync.SyncExecutor
import player.phonograph.repo.room.domain.BasicSyncExecutor
import androidx.room.withTransaction
import player.phonograph.repo.room.domain.RelationshipSyncExecutor
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context
import android.util.Log

object DatabaseActions {

    private suspend fun loopUpSyncExecutor(context: Context, musicDatabase: MusicDatabase): SyncExecutor {
        val mode = Setting(context)[Keys.musicLibraryBackend].read()
        val syncExecutor = when (mode) {
            PROVIDER_MEDIASTORE_PARSED -> RelationshipSyncExecutor(musicDatabase)
            else                       -> BasicSyncExecutor(musicDatabase)
        }
        return syncExecutor
    }

    private suspend fun deleteTablesOfNonUserData(musicDatabase: MusicDatabase) {
        musicDatabase.MediaStoreSongDao().deleteAll()
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
     * check MediaStore, refresh database if have changes
     */
    suspend fun sync(
        context: Context,
        musicDatabase: MusicDatabase,
        progress: ProgressNotificationConnection? = null,
        force: Boolean = false,
    ): SyncReport? {
        val syncExecutor = loopUpSyncExecutor(context, musicDatabase)
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
        progress: ProgressNotificationConnection? = null,
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
        progress: ProgressNotificationConnection? = null,
    ): SyncReport? {
        progress?.onStart(7328453)
        val wipeResult = wipe(context, musicDatabase, progress, includeUserData = false)
        val syncResult = sync(context, musicDatabase, progress, force = !wipeResult)
        progress?.onCompleted()
        return syncResult
    }

    private const val TAG = "Database"
}