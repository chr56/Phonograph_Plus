/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.event

import player.phonograph.App
import player.phonograph.PACKAGE_NAME
import player.phonograph.foundation.compat.MediaStoreCompat.Audio.Playlists
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.provider.MediaStore.Audio.Albums
import android.provider.MediaStore.Audio.Artists
import android.provider.MediaStore.Audio.Genres
import android.provider.MediaStore.Audio.Media

object EventHub {

    const val EVENT_MEDIASTORE_CHANGED = "${PACKAGE_NAME}.mediastore_changed"
    const val EVENT_PLAYLISTS_CHANGED = "${PACKAGE_NAME}.playlists_changed"
    const val EVENT_FAVORITES_CHANGED = "${PACKAGE_NAME}.favorites_changed"

    private fun localBroadcastManager(context: Context) = LocalBroadcastManager.getInstance(context.applicationContext)


    //region Send

    fun sendEvent(context: Context, event: String) =
        localBroadcastManager(context).sendBroadcast(Intent(event))


    //endregion


    //region Receive
    abstract class EventReceiver(val event: String) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == event) onEventReceived(context, intent)
        }

        abstract fun onEventReceived(context: Context, intent: Intent)

        fun registerSelf(context: Context) {
            localBroadcastManager(context).registerReceiver(this, IntentFilter(event))
        }

        fun unregisterSelf(context: Context) {
            localBroadcastManager(context).unregisterReceiver(this)
        }

    }

    fun EventReceiver(
        event: String,
        onEvent: (context: Context, intent: Intent) -> Unit,
    ): EventReceiver =
        object : EventReceiver(event) {
            override fun onEventReceived(context: Context, intent: Intent) = onEvent(context, intent)
        }

    abstract class LifeCycleEventReceiver(context: Context, event: String) :
            EventReceiver(event), DefaultLifecycleObserver {

        private val localBroadcastManager by lazy { localBroadcastManager(context) }

        override fun onCreate(owner: LifecycleOwner) {
            localBroadcastManager.registerReceiver(this, IntentFilter(event))
        }

        override fun onDestroy(owner: LifecycleOwner) {
            localBroadcastManager.unregisterReceiver(this)
        }

        fun registerWithLifecycle(lifecycle: Lifecycle) {
            lifecycle.addObserver(this)
        }
    }

    fun LifeCycleEventReceiver(
        context: Context,
        event: String,
        onEvent: (context: Context, intent: Intent) -> Unit,
    ): LifeCycleEventReceiver =
        object : LifeCycleEventReceiver(context, event) {
            override fun onEventReceived(context: Context, intent: Intent) = onEvent(context, intent)
        }
    //endregion


    //region Bridge
    private lateinit var mediaStoreObserver: MediaStoreObserver

    private class MediaStoreObserver(
        private val playerHandler: Handler,
    ) : ContentObserver(playerHandler), Runnable {

        override fun onChange(selfChange: Boolean) {
            // if a change is detected, remove any scheduled callback
            // then post a new one. This is intended to prevent closely
            // spaced events from generating multiple refresh calls
            playerHandler.removeCallbacks(this)
            playerHandler.postDelayed(this, REFRESH_DELAY)
        }

        override fun run() {
            sendEvent(App.instance, EVENT_MEDIASTORE_CHANGED)
        }

        companion object {
            // milliseconds to delay before calling refresh to aggregate events
            private const val REFRESH_DELAY: Long = 500
        }
    }

    fun setUpMediaStoreObserver(context: Context, playerHandler: Handler) {
        mediaStoreObserver = MediaStoreObserver(playerHandler)
        with(context.contentResolver) {
            registerContentObserver(Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Albums.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Artists.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Genres.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Playlists.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)

            registerContentObserver(Media.INTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Albums.INTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Artists.INTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Genres.INTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Playlists.INTERNAL_CONTENT_URI, true, mediaStoreObserver)
        }
    }

    fun unregisterMediaStoreObserver(context: Context) {
        context.contentResolver.unregisterContentObserver(mediaStoreObserver)
    }
    //endregion

}