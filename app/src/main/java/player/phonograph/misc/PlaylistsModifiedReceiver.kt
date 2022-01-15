/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.misc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED

class PlaylistsModifiedReceiver(private val callback: () -> Any) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action ?: "NONE") {
            "NONE" -> {} // do nothing
            BROADCAST_PLAYLISTS_CHANGED -> callback()
        }
    }
}
