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
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.databinding.DialogSleepTimerBinding
import com.kabouzeid.gramophone.helper.MusicPlayerRemote
import com.kabouzeid.gramophone.service.MusicService
import com.kabouzeid.gramophone.util.MusicUtil
import com.kabouzeid.gramophone.util.PreferenceUtil
import com.triggertrap.seekarc.SeekArc
import com.triggertrap.seekarc.SeekArc.OnSeekArcChangeListener

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class SleepTimerDialog : DialogFragment() {
    private var binding: DialogSleepTimerBinding? = null
    private val _binding get() = binding!!

    private var seekArcProgress = 0
    private lateinit var dialog: MaterialDialog
    private lateinit var timerUpdater: TimerUpdater

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        timerUpdater.cancel()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogSleepTimerBinding.inflate(LayoutInflater.from(requireContext()))
        timerUpdater = TimerUpdater()
        dialog = MaterialDialog(requireActivity())
            .title(R.string.action_sleep_timer)
            .positiveButton(R.string.action_set) {
                PreferenceUtil.getInstance(requireActivity()).sleepTimerFinishMusic = binding!!.shouldFinishLastSong.isChecked
                val minutes = seekArcProgress
                val pi = makeTimerPendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
                val nextSleepTimerElapsedTime = SystemClock.elapsedRealtime() + minutes * 60 * 1000
                PreferenceUtil.getInstance(requireActivity()).setNextSleepTimerElapsedRealtime(nextSleepTimerElapsedTime)
                val am = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am[AlarmManager.ELAPSED_REALTIME_WAKEUP, nextSleepTimerElapsedTime] = pi
                Toast.makeText(requireActivity(), requireActivity().resources.getString(R.string.sleep_timer_set, minutes), Toast.LENGTH_SHORT).show()
            }
            .negativeButton {
                val previous = makeTimerPendingIntent(PendingIntent.FLAG_NO_CREATE)
                if (previous != null) {
                    val am = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am.cancel(previous)
                    previous.cancel()
                    Toast.makeText(requireActivity(), requireActivity().resources.getString(R.string.sleep_timer_canceled), Toast.LENGTH_SHORT).show()
                }
                val musicService = MusicPlayerRemote.musicService
                if (musicService != null && musicService.pendingQuit) {
                    musicService.pendingQuit = false
                    Toast.makeText(requireActivity(), requireActivity().resources.getString(R.string.sleep_timer_canceled), Toast.LENGTH_SHORT).show()
                }
            }
            .neutralButton {
                val previous = makeTimerPendingIntent(PendingIntent.FLAG_NO_CREATE)
                if (previous != null) {
                    val am = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am.cancel(previous)
                    previous.cancel()
                    Toast.makeText(requireActivity(), requireActivity().resources.getString(R.string.sleep_timer_canceled), Toast.LENGTH_SHORT).show()
                }
                val musicService = MusicPlayerRemote.musicService
                if (musicService != null && musicService.pendingQuit) {
                    musicService.pendingQuit = false
                    Toast.makeText(requireActivity(), requireActivity().resources.getString(R.string.sleep_timer_canceled), Toast.LENGTH_SHORT).show()
                } 
            }
            .customView(viewRes = R.layout.dialog_sleep_timer, noVerticalPadding = true) // Todo
        dialog.setOnShowListener {
            if (makeTimerPendingIntent(PendingIntent.FLAG_NO_CREATE) != null) {
                timerUpdater.start()
            }
        }

        // View
        val seekArc = binding!!.seekArc
        val timerDisplay = binding!!.timerDisplay

        val finishMusic = PreferenceUtil.getInstance(requireActivity()).sleepTimerFinishMusic
        binding!!.shouldFinishLastSong.isChecked = finishMusic
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
        seekArcProgress = PreferenceUtil.getInstance(requireActivity()).lastSleepTimerValue
        updateTimeDisplayTime()

        seekArc.progress = seekArcProgress
        seekArc.setOnSeekArcChangeListener(ChangeListener())
        return dialog
    }

    private fun updateTimeDisplayTime() {
        binding!!.timerDisplay.text = "$seekArcProgress min"
    }

    private fun makeTimerPendingIntent(flag: Int): PendingIntent? {
        return PendingIntent.getService(requireActivity(), 0, makeTimerIntent(), flag)
    }

    private fun makeTimerIntent(): Intent {
        val intent = Intent(requireActivity(), MusicService::class.java)
        return if (binding!!.shouldFinishLastSong.isChecked) {
            intent.setAction(MusicService.ACTION_PENDING_QUIT)
        } else intent.setAction(MusicService.ACTION_QUIT)
    }

    private fun updateCancelButton() {
        val musicService = MusicPlayerRemote.musicService
        if (musicService != null && musicService.pendingQuit) {
            dialog.neutralButton(text = requireContext().getString(R.string.cancel_current_timer))
//            materialDialog.setActionButton(DialogAction.NEUTRAL, materialDialog!!.context.getString(R.string.cancel_current_timer))
//        } else {
//            materialDialog.setActionButton(DialogAction.NEUTRAL, null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private inner class TimerUpdater : CountDownTimer(PreferenceUtil.getInstance(requireActivity()).nextSleepTimerElapsedRealTime - SystemClock.elapsedRealtime(), 1000) {
        override fun onTick(millisUntilFinished: Long) {
            dialog.neutralButton(text = requireContext().getString(R.string.cancel_current_timer) + " (" + MusicUtil.getReadableDurationString(millisUntilFinished) + ")")
        }
        override fun onFinish() {
            updateCancelButton()
        }
    }
    private inner class ChangeListener : OnSeekArcChangeListener {
        override fun onProgressChanged(seekArc: SeekArc, i: Int, b: Boolean) {
            if (i < 1) {
                seekArc.progress = 1
                return
            }
            seekArcProgress = i
            updateTimeDisplayTime()
        }

        override fun onStartTrackingTouch(seekArc: SeekArc) {}
        override fun onStopTrackingTouch(seekArc: SeekArc) {
            PreferenceUtil.getInstance(requireActivity()).lastSleepTimerValue = seekArcProgress
        }
    }
}
