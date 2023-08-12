/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.event

import player.phonograph.MusicServiceMsgConst.MEDIA_STORE_CHANGED
import player.phonograph.util.registerReceiverCompat
import androidx.core.content.ContextCompat
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

lateinit var eventReceiver: EventReceiver
    private set

class EventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MEDIA_STORE_CHANGED -> MediaStoreTracker.dispatch()
        }
    }
}

private var initialed = false

fun setupEventReceiver(context: Context) {
    if (!initialed) {
        eventReceiver = EventReceiver()
        context.applicationContext.registerReceiverCompat(
            eventReceiver,
            IntentFilter(MEDIA_STORE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        initialed = true
    }
}