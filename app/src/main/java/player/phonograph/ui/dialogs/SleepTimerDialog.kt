/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.triggertrap.seekarc.SeekArc
import player.phonograph.App
import player.phonograph.R
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.util.SleepTimer
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.views.AccentColorCheckBox
import player.phonograph.util.text.readableDuration
import player.phonograph.util.theme.ThemeSettingsDelegate.accentColor
import player.phonograph.util.theme.tintAlertDialogButtons
import player.phonograph.util.theme.tintButtons
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
            .create().apply {
                val duration = SleepTimer.timerElapsedTime() - SystemClock.elapsedRealtime()
                timerUpdater = TimerUpdater(duration.coerceAtLeast(0))
                setOnShowListener {
                    tintAlertDialogButtons(it as AlertDialog)
                    if (SleepTimer.hasTimer()) timerUpdater.start()
                }
            }


        return dialog
    }


    override fun onStart() {
        super.onStart()
        setupMainView(dialog)
    }

    private fun startTimer() {
        val service = MusicPlayerRemote.accessMusicService()
        if (service == null) {
            showDisconnectedDialog()
            return
        }

        val minutesToQuit = progress.toLong()
        val shouldFinishLastSong = Setting(service)[Keys.sleepTimerFinishMusic].data
        if (SleepTimer.setTimer(service, minutesToQuit, shouldFinishLastSong)) {
            Toast.makeText(
                service,
                getString(R.string.msg_sleep_timer_set, minutesToQuit),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                service,
                getString(R.string.failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun cancelTimer() {
        val service = MusicPlayerRemote.accessMusicService()
        if (service == null) {
            showDisconnectedDialog()
            return
        }

        if (SleepTimer.cancelTimer(service)) {
            Toast.makeText(
                service,
                getString(R.string.msg_sleep_timer_canceled),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                service,
                getString(R.string.failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showDisconnectedDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.tips_service_disconnected)
            .create().tintButtons()
    }

    private fun setupMainView(alertDialog: AlertDialog) {

        // init views
        timeDisplay = alertDialog.findViewById(R.id.timer_display)!!
        val seekArc: SeekArc = alertDialog.findViewById(R.id.seek_arc)!!

        // init views : set seekArc color, size and progress
        seekArc.progressColor = accentColor()
        seekArc.setThumbColor(accentColor())
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
                    timeDisplay.text = String.format(getString(R.string.tips_minutes), i)
                }

                override fun onStartTrackingTouch(seekArc: SeekArc) {}
                override fun onStopTrackingTouch(seekArc: SeekArc) {
                    Setting(App.instance)[Keys.lastSleepTimerValue].data = seekArc.progress
                }
            })

        // init views : set checkBox basing on preference
        alertDialog
            .findViewById<AccentColorCheckBox>(R.id.should_finish_last_song)!!// To remember settings last use sleep-timer
            .apply {
                isChecked = Setting(context)[Keys.sleepTimerFinishMusic].data
                setOnCheckedChangeListener { _, isChecked ->
                    Setting(context)[Keys.sleepTimerFinishMusic].data = isChecked
                }
            }

        // init views : set remaining time for timeDisplay
        timeDisplay.text =
            getString(R.string.tips_minutes, Setting(requireContext())[Keys.lastSleepTimerValue].data)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        timerUpdater.cancel()
    }

    /**
     * A CountDownTimer to update UI
     */
    private inner class TimerUpdater(duration: Long) : CountDownTimer(duration, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            setNegativeButtonText(millisUntilFinished)
        }

        override fun onFinish() {
            setNegativeButtonText(0)
        }

        private fun setNegativeButtonText(time: Long) {
            val text = requireContext().getString(R.string.action_cancel_current_timer).plus(
                if (MusicPlayerRemote.isServiceConnected) {
                    if (time > 0 && SleepTimer.hasTimer()) "(${readableDuration(time)})" else ""
                } else {
                    requireContext().getString(R.string.tips_service_disconnected)
                }
            )
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.text = text
        }
    }
}
