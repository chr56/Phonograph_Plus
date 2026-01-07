/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.App
import player.phonograph.R
import player.phonograph.foundation.error.InternalDataCorruptedException
import player.phonograph.foundation.notification.ProgressNotificationConnection
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_DATABASE_SYNC
import player.phonograph.model.repo.sync.SyncProcessor
import player.phonograph.repo.room.entity.Metadata
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class RoomSyncProcessor(
    private val database: MusicDatabase,
    private val coroutineScope: CoroutineScope,
) : SyncProcessor {
    private val syncFlag = AtomicBoolean(false)
    private val lastSync = AtomicLong(0)

    override fun onSyncFirstTime(context: Context) {
        coroutineScope.launch { execute(context) }
    }

    override fun onSyncTriggered(context: Context) {
        coroutineScope.launch { execute(context) }
    }

    private suspend fun execute(context: Context) {
        val current = System.currentTimeMillis() / INTERVAL_UNIT
        val last = lastSync.get()
        if (current - last > THROTTLE_INTERVAL) {
            if (lastSync.compareAndSet(last, current)
                && syncFlag.compareAndSet(false, true)
            ) {
                impl(context)
                syncFlag.set(false)
            }
        } else {
            delay(THROTTLE_INTERVAL * INTERVAL_UNIT)
            onSyncTriggered(context)
        }
    }

    private suspend fun impl(context: Context) {
        if (database.isOpen) {
            val progress = ProgressNotificationConnection(
                context, R.string.action_refresh_database,
                channel = NOTIFICATION_CHANNEL_ID_DATABASE_SYNC,
            )
            progress.onStart()
            DatabaseActions.sync(context, database, progress)
            progress.onCompleted()
        }
    }

    companion object {
        private const val THROTTLE_INTERVAL = 8L
        private const val INTERVAL_UNIT = 100

        fun observeMediastoreForSync(context: Context, musicDatabase: MusicDatabase): EventHub.EventReceiver {

            class PersistentEventListener(private val syncProcessor: SyncProcessor) :
                    EventHub.EventReceiver(EventHub.EVENT_MEDIASTORE_CHANGED) {
                override fun onEventReceived(context: Context, intent: Intent) {
                    syncProcessor.onSyncTriggered(context)
                }
            }

            val coroutineScope = (context.applicationContext as? App)?.appScope
                ?: CoroutineScope(Dispatchers.IO + SupervisorJob())

            coroutineScope.launch {
                writeAccessTime(musicDatabase)
            }

            val processor = RoomSyncProcessor(musicDatabase, coroutineScope).also {
                it.onSyncFirstTime(context)
            }
            return PersistentEventListener(processor).also { it.registerSelf(context) }
        }

        private fun writeAccessTime(musicDatabase: MusicDatabase) = try {
            musicDatabase.MetadataDao().insertOrReplace(
                Metadata(Metadata.METADATA_ACCESS_TIME, System.currentTimeMillis().toString())
            )
        } catch (e: IllegalStateException) {
            val hint = if (e.message?.contains("data integrity") ?: false) {
                "Database is corrupted! schema version mismatched!"
            } else {
                "Database is corrupted!"
            }
            throw InternalDataCorruptedException(hint, e)
        }
    }
}