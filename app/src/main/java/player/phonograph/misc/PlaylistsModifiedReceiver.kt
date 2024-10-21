/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.misc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import player.phonograph.PACKAGE_NAME
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.IntentFilter

abstract class PlaylistsModifiedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_BROADCAST_PLAYLISTS_CHANGED -> onPlaylistChanged(context, intent)
        }
    }

    abstract fun onPlaylistChanged(context: Context, intent: Intent)

    companion object {
        const val ACTION_BROADCAST_PLAYLISTS_CHANGED = "$PACKAGE_NAME.playlists_changed"

        val filter: IntentFilter = IntentFilter(ACTION_BROADCAST_PLAYLISTS_CHANGED)

        fun sendBroadcastLocally(context: Context) =
            LocalBroadcastManager.getInstance(context.applicationContext)
                .sendBroadcast(Intent(ACTION_BROADCAST_PLAYLISTS_CHANGED))
    }
}
