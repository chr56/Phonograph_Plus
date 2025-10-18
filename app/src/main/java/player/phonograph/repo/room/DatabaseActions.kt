/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.R
import player.phonograph.foundation.notification.ProgressNotificationConnection
import player.phonograph.model.repo.sync.SyncResult
import player.phonograph.repo.room.domain.BasicSyncExecutor
import android.content.Context

object DatabaseActions {

    /**
     * Sync Database:
     * check MediaStore, refresh database if have changes
     */
    suspend fun sync(context: Context, musicDatabase: MusicDatabase, force: Boolean = false): SyncResult? {

        val syncExecutor = BasicSyncExecutor(musicDatabase)

        return if (force || syncExecutor.check(context)) {
            val connection = ProgressNotificationConnection(context, R.string.action_refresh_database)
            try {
                connection.onStart()
                syncExecutor.sync(context, connection)
            } finally {
                connection.onCompleted()
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