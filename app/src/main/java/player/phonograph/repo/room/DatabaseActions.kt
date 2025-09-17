/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.R
import player.phonograph.foundation.notification.BackgroundNotification
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.repo.sync.SyncExecutor
import player.phonograph.model.repo.sync.SyncResult
import player.phonograph.repo.room.domain.BasicSyncExecutor
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DatabaseActions {

    fun syncWithMediastore(context: Context, musicDatabase: MusicDatabase): EventHub.EventReceiver {
        class PersistentListener : EventHub.EventReceiver(EventHub.EVENT_MEDIASTORE_CHANGED) {

            override fun onEventReceived(context: Context, intent: Intent) {
                refresh()
            }

            fun refresh() {
                coroutineScope.launch {
                    if (musicDatabase.isOpen) checkAndRefresh(context, musicDatabase)
                }
            }

            val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        }

        return PersistentListener().apply {
            refresh()
            registerSelf(context)
        }
    }


    /**
     * Sync Database:
     * check MediaStore, refresh database if have changes
     */
    suspend fun checkAndRefresh(context: Context, musicDatabase: MusicDatabase): SyncResult? {

        val syncExecutor = BasicSyncExecutor(musicDatabase)

        return if (syncExecutor.check(context)) {
            val connection = SyncProgressNotificationConnection(
                context, System.currentTimeMillis().mod(172_800_000)
            )
            try {
                syncExecutor.sync(context, connection)
            } finally {
                connection.onEnd()
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

    class SyncProgressNotificationConnection(val context: Context, val id: Int) : SyncExecutor.SyncProgressConnection {
        private val title = context.getString(R.string.action_refresh_database)
        private val subtitle = context.getString(R.string.state_updating)
        override fun onProcessUpdate(message: String?) {
            if (message != null) BackgroundNotification.post(context, title, message, id)
        }

        override fun onProcessUpdate(current: Int, total: Int) {
            BackgroundNotification.post(context, title, subtitle, id, current, total)
        }

        override fun onProcessUpdate(current: Int, total: Int, message: String?) {
            BackgroundNotification.post(context, "$title: $message", title, id, current, total)
        }

        fun onEnd() {
            BackgroundNotification.remove(context, id)
        }
    }

}