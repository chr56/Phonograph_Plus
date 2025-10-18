/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.App
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.repo.sync.SyncProcessor
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RoomSyncProcessor(
    private val database: MusicDatabase,
    private val coroutineScope: CoroutineScope,
) : SyncProcessor {

    override fun onSyncFirstTime(context: Context) {
        execute(context)
    }

    override fun onSyncTriggered(context: Context) {
        execute(context)
    }

    private fun execute(context: Context) {
        coroutineScope.launch {
            if (database.isOpen) DatabaseActions.sync(context, database)
        }
    }

    companion object {

        fun observeMediastoreForSync(context: Context, musicDatabase: MusicDatabase): EventHub.EventReceiver {

            class PersistentEventListener(private val syncProcessor: SyncProcessor) :
                    EventHub.EventReceiver(EventHub.EVENT_MEDIASTORE_CHANGED) {
                override fun onEventReceived(context: Context, intent: Intent) {
                    syncProcessor.onSyncTriggered(context)
                }
            }

            val coroutineScope = (context.applicationContext as? App)?.appScope
                ?: CoroutineScope(Dispatchers.IO + SupervisorJob())
            val processor = RoomSyncProcessor(musicDatabase, coroutineScope).also {
                it.onSyncFirstTime(context)
            }
            return PersistentEventListener(processor).also { it.registerSelf(context) }
        }
    }
}