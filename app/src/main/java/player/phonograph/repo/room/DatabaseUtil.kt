/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.mechanism.event.EventHub
import player.phonograph.repo.room.domain.DatabaseSync
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DatabaseUtil {

    fun syncWithMediastore(context: Context, musicDatabase: MusicDatabase): EventHub.EventReceiver {
        class PersistentListener : EventHub.EventReceiver(EventHub.EVENT_MEDIASTORE_CHANGED) {

            override fun onEventReceived(context: Context, intent: Intent) {
                refresh()
            }


            fun refresh() {
                coroutineScope.launch {
                    DatabaseSync.checkAndRefresh(context, musicDatabase)
                }
            }

            val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        }

        return PersistentListener().apply {
            refresh()
            registerSelf(context)
        }
    }


    suspend fun deleteEntireDatabase(context: Context, musicDatabase: MusicDatabase) = withContext(Dispatchers.IO) {
        val path = context.getDatabasePath(MusicDatabase.DATABASE_NAME)
        try {
            synchronized(musicDatabase) {
                musicDatabase.close()
                path.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}