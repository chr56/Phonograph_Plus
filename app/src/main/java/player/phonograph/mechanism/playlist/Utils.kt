/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist

import org.koin.core.context.GlobalContext
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.settings.Keys
import player.phonograph.settings.PLAYLIST_OPS_BEHAVIOUR_AUTO
import player.phonograph.settings.PLAYLIST_OPS_BEHAVIOUR_FORCE_LEGACY
import player.phonograph.settings.PLAYLIST_OPS_BEHAVIOUR_FORCE_SAF
import player.phonograph.settings.Setting
import android.content.Context
import android.os.Build

fun shouldUseSAF(context: Context): Boolean {
    val preference = Setting(context)[Keys.playlistFilesOperationBehaviour]
    return when (preference.data) {
        PLAYLIST_OPS_BEHAVIOUR_FORCE_SAF    -> true
        PLAYLIST_OPS_BEHAVIOUR_FORCE_LEGACY -> false
        PLAYLIST_OPS_BEHAVIOUR_AUTO         -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        else                                -> {
            preference.data = PLAYLIST_OPS_BEHAVIOUR_AUTO // reset to default
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        }
    }
}

fun notifyMediaStoreChanged() = GlobalContext.get().get<MediaStoreTracker>().notifyAllListeners()