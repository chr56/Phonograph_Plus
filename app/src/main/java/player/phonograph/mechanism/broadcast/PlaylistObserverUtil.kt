/*
 *  Copyright (c) 2022~2024 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mechanism.broadcast

import player.phonograph.App
import player.phonograph.PACKAGE_NAME
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

fun sentPlaylistChangedLocalBoardCast() = PlaylistsModifiedReceiver.sendBroadcastLocally(App.Companion.instance)

abstract class PlaylistsModifiedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_BROADCAST_PLAYLISTS_CHANGED -> onPlaylistChanged(context, intent)
        }
    }

    abstract fun onPlaylistChanged(context: Context, intent: Intent)

    companion object {

        val filter: IntentFilter = IntentFilter(ACTION_BROADCAST_PLAYLISTS_CHANGED)

        fun sendBroadcastLocally(context: Context) =
            LocalBroadcastManager.getInstance(context.applicationContext)
                .sendBroadcast(Intent(ACTION_BROADCAST_PLAYLISTS_CHANGED))
    }
}

private const val ACTION_BROADCAST_PLAYLISTS_CHANGED = "$PACKAGE_NAME.playlists_changed"
