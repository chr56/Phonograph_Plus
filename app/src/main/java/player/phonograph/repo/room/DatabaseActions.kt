/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.R
import player.phonograph.foundation.notification.ProgressNotificationConnection
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.repo.PROVIDER_MEDIASTORE_MIRROR
import player.phonograph.model.repo.PROVIDER_MEDIASTORE_PARSED
import player.phonograph.model.repo.sync.SyncExecutor
import player.phonograph.model.repo.sync.ProgressConnection
import player.phonograph.model.repo.sync.SyncResult
import player.phonograph.repo.room.domain.BasicSyncExecutor
import player.phonograph.repo.room.domain.RelationshipSyncExecutor
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context

object DatabaseActions {

    /**
     * Sync Database:
     * check MediaStore, refresh database if have changes
     */
    suspend fun sync(context: Context, musicDatabase: MusicDatabase, force: Boolean = false): SyncResult? {

        val mode = Setting(context)[Keys.musicLibraryBackend].read()
        val syncExecutor = when (mode) {
            PROVIDER_MEDIASTORE_PARSED -> RelationshipSyncExecutor(musicDatabase)
            else                       -> BasicSyncExecutor(musicDatabase)
        }

        return if (force || syncExecutor.check(context)) {
            val connection = ProgressNotificationConnection(context, R.string.action_refresh_database)
            try {
                connection.onStart()
                syncExecutor.sync(context, connection)
            } finally {
                connection.onCompleted()
                EventHub.sendEvent(context.applicationContext, EventHub.EVENT_MUSIC_LIBRARY_CHANGED)
            }
        } else {
            null
        }

    }

    /**
     * Close Database and wipe all table
     *
     * **Require reboot after operation**
     *
     */
    fun wipe(musicDatabase: MusicDatabase) = run {
        synchronized(musicDatabase) {
            musicDatabase.close()
            musicDatabase.clearAllTables()
        }
    }

    /**
     * Close Database delete database file
     *
     * **Require reboot after operation**
     *
     * @return deletion result
     */
    fun purge(context: Context, musicDatabase: MusicDatabase): Boolean = run {
        val path = context.getDatabasePath(MusicDatabase.DATABASE_NAME)
        synchronized(musicDatabase) {
            musicDatabase.close()
            path.delete()
        }
    }

}