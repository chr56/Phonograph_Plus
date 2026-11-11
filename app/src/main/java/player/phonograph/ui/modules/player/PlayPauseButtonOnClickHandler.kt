/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.player

import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.modules.panel.AbsMusicServiceActivity
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.view.View
import kotlinx.coroutines.launch

class PlayPauseButtonOnClickHandler : View.OnClickListener {
    override fun onClick(v: View) {
        if (!MusicPlayerRemote.togglePlayPauseSafely()) {
            rebind(v.context)
        }
    }

    private fun rebind(activity: Context?) {
        if (activity is AbsMusicServiceActivity) {
            activity.lifecycleScope.launch {
                activity.connectToService()
            }
        }
    }
}