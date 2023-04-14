/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.triggertrap.seekarc.SeekArc
import lib.phonograph.view.CheckBoxX
import mt.pref.ThemeColor.accentColor
import player.phonograph.R
import player.phonograph.model.getReadableDurationString
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.util.SleepTimer
import player.phonograph.settings.Setting
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast

/**
 * @author Karim Abou Zeid (kabouzeid), chr_56<modify>
 */
class SleepTimerDialog : DialogFragment() {

    private lateinit var dialog: AlertDialog
    private lateinit var timerUpdater: TimerUpdater
    private lateinit var timeDisplay: TextView
    private var progress: Int = 1

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_sleep_timer)
            .setPositiveButton(R.string.action_set) { _, _ ->
                startTimer()
            }
            .setNegativeButton(android.R.string.cancel) { it, _ ->
                cancelTimer()
                it.dismiss()
            }
            .setView(R.layout.dialog_sleep_timer)
            .create().also {
                timerUpdater = TimerUpdater()
                it.setOnShowListener { alterDialog ->
                    alterDialog as AlertDialog
                    alterDialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(accentColor)
                    alterDialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(accentColor)

                    val service = MusicPlayerRemote.musicService ?: return@setOnShowListener
                    if (SleepTimer.instance(service).hasTimer()) timerUpdater.start()
                }
            }


        return dialog
    }


    override fun onStart() {
        super.onStart()
        setupMainView(dialog)
    }

    private fun startTimer() {
        val minutesToQuit = progress
        val service = MusicPlayerRemote.musicService
        require(service != null)

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

    private fun cancelTimer() {
        val service = MusicPlayerRemote.musicService
        require(service != null)

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

    private fun setupMainView(alertDialog: AlertDialog) {

        // init views
        timeDisplay = alertDialog.findViewById(R.id.timer_display)!!
        val seekArc: SeekArc = alertDialog.findViewById(R.id.seek_arc)!!

        // init views : set seekArc color, size and progress
        seekArc.progressColor = accentColor
        seekArc.setThumbColor(accentColor)
        seekArc.post {
            val width = seekArc.width
            val height = seekArc.height
            val small = width.coerceAtMost(height)
            val layoutParams = FrameLayout.LayoutParams(seekArc.layoutParams)
            layoutParams.height = small
            seekArc.layoutParams = layoutParams
        }
        seekArc.progress = progress
        seekArc.setOnSeekArcChangeListener(
            object : SeekArc.OnSeekArcChangeListener {
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
        alertDialog
            .findViewById<CheckBoxX>(R.id.should_finish_last_song)!!// To remember settings last use sleep-timer
            .apply {
                isChecked = Setting.instance.sleepTimerFinishMusic
                setOnCheckedChangeListener { _, isChecked ->
                    Setting.instance.sleepTimerFinishMusic = isChecked
                }
            }

        // init views : set remaining time for timeDisplay
        timeDisplay.text =
            getString(R.string.minutes_short, Setting.instance.lastSleepTimerValue)
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
                    if (time > 0 && SleepTimer.instance(it).hasTimer()) "(${getReadableDurationString(time)})" else ""
                } ?: "(N/A)"
            )
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.text = text
        }
    }

    val accentColor get() = accentColor(requireContext())
}
