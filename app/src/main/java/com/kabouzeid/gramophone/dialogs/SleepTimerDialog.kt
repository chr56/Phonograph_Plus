package com.kabouzeid.gramophone.dialogs

import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.helper.MusicPlayerRemote
import com.kabouzeid.gramophone.service.MusicService
import com.kabouzeid.gramophone.util.MusicUtil
import com.kabouzeid.gramophone.util.PreferenceUtil
import com.kabouzeid.gramophone.views.basic.CheckBoxX
import com.triggertrap.seekarc.SeekArc

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

                    val nextSleepTimerElapsedTime = SystemClock.elapsedRealtime() + minutesToQuit * 60 * 1000
                    PreferenceUtil.getInstance(requireActivity()).setNextSleepTimerElapsedRealtime(nextSleepTimerElapsedTime)

                    val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = makeTimerPendingIntent(requireContext(),
                            PreferenceUtil.getInstance(requireActivity()).sleepTimerFinishMusic,
                            PendingIntent.FLAG_CANCEL_CURRENT)

                    alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextSleepTimerElapsedTime,intent)
                    Toast.makeText(requireActivity(), requireActivity().resources.getString(R.string.sleep_timer_set, minutesToQuit), Toast.LENGTH_SHORT).show()
                }
                .negativeButton{
                    val previous = makeTimerPendingIntent(requireContext(),
                            PreferenceUtil.getInstance(requireActivity()).sleepTimerFinishMusic,
                            PendingIntent.FLAG_CANCEL_CURRENT)
                    if (previous != null) {
                        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        alarmManager.cancel(previous)
                        previous.cancel()
                        Toast.makeText(requireActivity(), requireActivity().resources.getString(R.string.sleep_timer_canceled), Toast.LENGTH_SHORT).show()
                    }
                    val musicService = MusicPlayerRemote.musicService
                    if (musicService != null && musicService.pendingQuit) {
                        musicService.pendingQuit = false
                        Toast.makeText(requireActivity(), requireActivity().resources.getString(R.string.sleep_timer_canceled), Toast.LENGTH_SHORT).show()
                    }
                }
                .customView(viewRes = R.layout.dialog_sleep_timer, noVerticalPadding = true)//Todo

        //set dialog button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))

        // get time to quit
        val minutesToQuit = PreferenceUtil.getInstance(requireActivity()).lastSleepTimerValue
        progress = minutesToQuit


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
        seekArc.setOnSeekArcChangeListener(ChangeListener())

        // init views : set checkBox basing on preference
        val checkBox: CheckBoxX = dialog.getCustomView().findViewById(R.id.should_finish_last_song) // To remember settings last use sleeptimer
        checkBox.isChecked = PreferenceUtil.getInstance(requireActivity()).sleepTimerFinishMusic
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            PreferenceUtil.getInstance(requireActivity()).sleepTimerFinishMusic = isChecked
        }

        // init views : set remaining time for timeDisplay
        timeDisplay.text = String.format(getString(R.string.minutes_short),PreferenceUtil.getInstance(requireActivity()).lastSleepTimerValue)

        // set up countdown timer
        timerUpdater = TimerUpdater()
        dialog.setOnShowListener {
            if (makeTimerPendingIntent(requireContext(),checkBox.isChecked,PendingIntent.FLAG_NO_CREATE) != null) {
                timerUpdater.start()
            }
        }
        return dialog
    }
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        timerUpdater.cancel()
    }


    // inner classes
    private inner class TimerUpdater : CountDownTimer(PreferenceUtil.getInstance(requireActivity()).nextSleepTimerElapsedRealTime - SystemClock.elapsedRealtime(), 1000) {
        override fun onTick(millisUntilFinished: Long) {
            dialog.negativeButton(text = requireContext().getString(R.string.cancel_current_timer) + " (" + MusicUtil.getReadableDurationString(millisUntilFinished) + ")")
        }
        override fun onFinish() {
            val musicService = MusicPlayerRemote.musicService
            if (musicService != null && musicService.pendingQuit) {
                dialog.negativeButton(text = requireContext().getString(R.string.cancel_current_timer))
            }
        }
    }
    private inner class ChangeListener : SeekArc.OnSeekArcChangeListener {
        override fun onProgressChanged(seekArc: SeekArc, i: Int, b: Boolean) {
            progress = if(i < 1) 1 else i
            timeDisplay.text = String.format(getString(R.string.minutes_short),i)
        }

        override fun onStartTrackingTouch(seekArc: SeekArc) {}
        override fun onStopTrackingTouch(seekArc: SeekArc) {
            PreferenceUtil.getInstance(requireActivity()).lastSleepTimerValue = seekArc.progress
        }
    }

    // companion object
    companion object{
        private fun makeTimerIntent(context: Context, shouldFinishLastSong: Boolean): Intent {
            val intent = Intent(context, MusicService::class.java)
            return if (shouldFinishLastSong) {
                intent.setAction(MusicService.ACTION_PENDING_QUIT)
            } else intent.setAction(MusicService.ACTION_QUIT)
        }
        private fun makeTimerPendingIntent(context: Context, shouldFinishLastSong: Boolean, flag: Int): PendingIntent? {
            return PendingIntent.getService(context, 0, makeTimerIntent(context, shouldFinishLastSong), flag)
        }
    }
}
