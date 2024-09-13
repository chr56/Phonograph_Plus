/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.fragments.player

import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.activities.base.AbsMusicServiceActivity
import androidx.lifecycle.lifecycleScope
import android.view.View
import kotlinx.coroutines.launch

class PlayPauseButtonOnClickHandler : View.OnClickListener {
    override fun onClick(v: View) {
        if (MusicPlayerRemote.musicService != null) {
            if (MusicPlayerRemote.isPlaying) {
                MusicPlayerRemote.pauseSong()
            } else {
                MusicPlayerRemote.resumePlaying()
            }
        } else {
            val activity = v.context
            if (activity is AbsMusicServiceActivity) {
                activity.lifecycleScope.launch {
                    activity.connectToService()
                }
            }
        }
    }
}