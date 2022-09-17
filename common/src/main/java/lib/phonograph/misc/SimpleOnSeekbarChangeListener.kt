package lib.phonograph.misc

import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class SimpleOnSeekbarChangeListener : OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
}
