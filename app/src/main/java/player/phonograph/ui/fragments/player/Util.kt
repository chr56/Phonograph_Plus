/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.player

import player.phonograph.service.MusicPlayerRemote
import android.view.View

class PlayPauseButtonOnClickHandler : View.OnClickListener {
    override fun onClick(v: View) {
        if (MusicPlayerRemote.isPlaying) {
            MusicPlayerRemote.pauseSong()
        } else {
            MusicPlayerRemote.resumePlaying()
        }
    }
}