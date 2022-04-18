package player.phonograph.helper

import android.view.View
import androidx.annotation.Keep

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@Keep
class PlayPauseButtonOnClickHandler : View.OnClickListener {
    override fun onClick(v: View) {
        if (MusicPlayerRemote.isPlaying) {
            MusicPlayerRemote.pauseSong()
        } else {
            MusicPlayerRemote.resumePlaying()
        }
    }
}