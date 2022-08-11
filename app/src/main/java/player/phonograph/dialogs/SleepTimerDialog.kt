package player.phonograph.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.triggertrap.seekarc.SeekArc
import lib.phonograph.view.CheckBoxX
import player.phonograph.R
import player.phonograph.model.getReadableDurationString
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.util.SleepTimer
import player.phonograph.settings.Setting
import util.mdcolor.pref.ThemeColor

/**
 * @author Karim Abou Zeid (kabouzeid), chr_56<modify>
 */
class SleepTimerDialog : DialogFragment() {
    private lateinit var dialog: MaterialDialog
    private lateinit var timerUpdater: TimerUpdater
    private lateinit var timeDisplay: TextView
    private var progress: Int = 10

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = MaterialDialog(requireContext())
            .title(R.string.action_sleep_timer)
            .positiveButton(R.string.action_set) {
                val minutesToQuit = progress
                val service = MusicPlayerRemote.musicService ?: return@positiveButton

                SleepTimer.instance(service).setTimer(
                    minutesToQuit.toLong(),
                    Setting.instance.sleepTimerFinishMusic
                ).let { success ->
                    Toast.makeText(
                        requireActivity(),
                        if (success) {
                            getString(R.string.sleep_timer_set, minutesToQuit)
                        } else {
                            getString(R.string.failed)
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .negativeButton {
                val service = MusicPlayerRemote.musicService ?: return@negativeButton
                SleepTimer.instance(service).cancelTimer().let {
                    Toast.makeText(
                        requireActivity(),
                        if (it) {
                            getString(R.string.sleep_timer_canceled)
                        } else {
                            getString(R.string.failed)
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .apply {
                // set dialog button color
                getActionButton(WhichButton.POSITIVE).updateTextColor(
                    ThemeColor.accentColor(requireActivity())
                )
                getActionButton(WhichButton.NEGATIVE).updateTextColor(
                    ThemeColor.accentColor(requireActivity())
                )
            }
            .customView(viewRes = R.layout.dialog_sleep_timer, noVerticalPadding = true)

        // get time to quit
        progress = Setting.instance.lastSleepTimerValue

        // init views
        val seekArc: SeekArc = dialog.getCustomView().findViewById(R.id.seek_arc)
        timeDisplay = dialog.getCustomView().findViewById(R.id.timer_display)

        // init views : set seekArc color, size and progress
        seekArc.progressColor = ThemeColor.accentColor(requireActivity())
        seekArc.setThumbColor(ThemeColor.accentColor(requireActivity()))
        seekArc.post {
            val width = seekArc.width
            val height = seekArc.height
            val small = width.coerceAtMost(height)
            val layoutParams = FrameLayout.LayoutParams(seekArc.layoutParams)
            layoutParams.height = small
            seekArc.layoutParams = layoutParams
        }
        seekArc.progress = progress
        seekArc.setOnSeekArcChangeListener(object : SeekArc.OnSeekArcChangeListener {
            override fun onProgressChanged(seekArc: SeekArc, i: Int, b: Boolean) {
                progress = if (i < 1) 1 else i
                timeDisplay.text = String.format(getString(R.string.minutes_short), i)
            }

            override fun onStartTrackingTouch(seekArc: SeekArc) {}
            override fun onStopTrackingTouch(seekArc: SeekArc) {
                Setting.instance.lastSleepTimerValue = seekArc.progress
            }
        })

        // init views : set checkBox basing on preference
        dialog.getCustomView().findViewById<CheckBoxX?>(R.id.should_finish_last_song) // To remember settings last use sleeptimer
            .apply {
                isChecked = Setting.instance.sleepTimerFinishMusic
                setOnCheckedChangeListener { _, isChecked ->
                    Setting.instance.sleepTimerFinishMusic = isChecked
                }
            }

        // init views : set remaining time for timeDisplay
        timeDisplay.text =
            getString(R.string.minutes_short, Setting.instance.lastSleepTimerValue)

        // set up countdown timer
        timerUpdater = TimerUpdater()
        dialog.setOnShowListener {
            val service = MusicPlayerRemote.musicService ?: return@setOnShowListener
            if (SleepTimer.instance(service).hasTimer()) timerUpdater.start()
        }
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        timerUpdater.cancel()
    }

    /**
     * A CountDownTimer to update UI
     */
    private inner class TimerUpdater : CountDownTimer(
        Setting.instance.nextSleepTimerElapsedRealTime - SystemClock.elapsedRealtime(),
        1000
    ) {
        override fun onTick(millisUntilFinished: Long) {
            setNegativeButtonText(millisUntilFinished)
        }
        override fun onFinish() {
            setNegativeButtonText(0)
        }
        private fun setNegativeButtonText(time: Long) {
            val text = requireContext().getString(R.string.cancel_current_timer).plus(
                MusicPlayerRemote.musicService?.let {
                    if (time > 0 && SleepTimer.instance(it).hasTimer()) {
                        "(${getReadableDurationString(time)})"
                    } else ""
                } ?: "(N/A)"
            )
            dialog.negativeButton(text = text)
        }
    }
}
