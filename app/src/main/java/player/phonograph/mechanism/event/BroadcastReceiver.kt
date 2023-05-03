/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.event

import player.phonograph.MusicServiceMsgConst.MEDIA_STORE_CHANGED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.applicationContext.registerReceiver(
                eventReceiver,
                IntentFilter(MEDIA_STORE_CHANGED),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.applicationContext.registerReceiver(
                eventReceiver,
                IntentFilter(MEDIA_STORE_CHANGED)
            )
        }
        initialed = true
    }
}