/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.event

import player.phonograph.PACKAGE_NAME
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

object EventHub {

    const val EVENT_MUSIC_LIBRARY_CHANGED = "${PACKAGE_NAME}.music_library_changed"
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

}