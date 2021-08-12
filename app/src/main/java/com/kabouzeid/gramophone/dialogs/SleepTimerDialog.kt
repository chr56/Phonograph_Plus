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
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import butterknife.BindView
import butterknife.ButterKnife
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.*
import com.afollestad.materialdialogs.actions.*
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.helper.MusicPlayerRemote
import com.kabouzeid.gramophone.service.MusicService
import com.kabouzeid.gramophone.util.MusicUtil
import com.kabouzeid.gramophone.util.PreferenceUtil
import com.triggertrap.seekarc.SeekArc
import com.triggertrap.seekarc.SeekArc.OnSeekArcChangeListener

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class SleepTimerDialog : DialogFragment()  {
    @JvmField
    @BindView(R.id.seek_arc)
    var seekArc: SeekArc? = null

    @JvmField
    @BindView(R.id.timer_display)
    var timerDisplay: TextView? = null

    @JvmField
    @BindView(R.id.should_finish_last_song)
    var shouldFinishLastSong: CheckBox? = null
    private var seekArcProgress = 0
    private lateinit var materialDialog: MaterialDialog
    private var timerUpdater: TimerUpdater? = null
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        timerUpdater!!.cancel()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        timerUpdater = TimerUpdater()
        materialDialog = MaterialDialog(requireActivity())
                .title(R.string.action_sleep_timer)
                .positiveButton(R.string.action_set){ dialog ->
                    if (requireActivity() == null) {
                        return@positiveButton
                    }
                    PreferenceUtil.getInstance(requireActivity()).sleepTimerFinishMusic = shouldFinishLastSong!!.isChecked
                    val minutes = seekArcProgress
                    val pi = makeTimerPendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
                    val nextSleepTimerElapsedTime = SystemClock.elapsedRealtime() + minutes * 60 * 1000
                    PreferenceUtil.getInstance(requireActivity()).setNextSleepTimerElapsedRealtime(nextSleepTimerElapsedTime)
                    val am = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am[AlarmManager.ELAPSED_REALTIME_WAKEUP, nextSleepTimerElapsedTime] = pi
                    Toast.makeText(requireActivity(), requireActivity().resources.getString(R.string.sleep_timer_set, minutes), Toast.LENGTH_SHORT).show()
                }
                .negativeButton{ dialog ->
                    if (requireActivity() == null) {
                        return@negativeButton
                    }
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
//                .showListener { dialog ->
//                    if (makeTimerPendingIntent(PendingIntent.FLAG_NO_CREATE) != null) {
//                        timerUpdater!!.start()
//                    }
//                }
                .customView(viewRes = R.layout.dialog_sleep_timer, dialogWrapContent = false)//Todo
        if (requireActivity() == null || materialDialog.getCustomView() == null) {
            return materialDialog!!
        }
        ButterKnife.bind(this, materialDialog.getCustomView())
        val finishMusic = PreferenceUtil.getInstance(requireActivity()).sleepTimerFinishMusic
        shouldFinishLastSong!!.isChecked = finishMusic
        seekArc!!.progressColor = ThemeColor.accentColor(requireActivity())
        seekArc!!.setThumbColor(ThemeColor.accentColor(requireActivity()))
        seekArc!!.post {
            val width = seekArc!!.width
            val height = seekArc!!.height
            val small = Math.min(width, height)
            val layoutParams = FrameLayout.LayoutParams(seekArc!!.layoutParams)
            layoutParams.height = small
            seekArc!!.layoutParams = layoutParams
        }
        seekArcProgress = PreferenceUtil.getInstance(requireActivity()).lastSleepTimerValue
        updateTimeDisplayTime()
        seekArc!!.progress = seekArcProgress
        seekArc!!.setOnSeekArcChangeListener(object : OnSeekArcChangeListener {
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
        })
        return materialDialog!!
    }

    private fun updateTimeDisplayTime() {
        timerDisplay!!.text = "$seekArcProgress min"
    }

    private fun makeTimerPendingIntent(flag: Int): PendingIntent? {
        return PendingIntent.getService(requireActivity(), 0, makeTimerIntent(), flag)
    }

    private fun makeTimerIntent(): Intent {
        val intent = Intent(requireActivity(), MusicService::class.java)
        return if (shouldFinishLastSong!!.isChecked) {
            intent.setAction(MusicService.ACTION_PENDING_QUIT)
        } else intent.setAction(MusicService.ACTION_QUIT)
    }

    private fun updateCancelButton() {
        val musicService = MusicPlayerRemote.musicService
        if (musicService != null && musicService.pendingQuit) {
//            materialDialog.setActionButton(DialogAction.NEUTRAL, materialDialog!!.context.getString(R.string.cancel_current_timer))
        } else {
//            materialDialog.setActionButton(DialogAction.NEUTRAL, null)
        }
    }

    private inner class TimerUpdater : CountDownTimer(PreferenceUtil.getInstance(requireActivity()).nextSleepTimerElapsedRealTime - SystemClock.elapsedRealtime(), 1000) {
        override fun onTick(millisUntilFinished: Long) {
//            materialDialog.setActionButton(DialogAction.NEUTRAL, materialDialog!!.context.getString(R.string.cancel_current_timer) + " (" + MusicUtil.getReadableDurationString(millisUntilFinished) + ")")
        }

        override fun onFinish() {
            updateCancelButton()
        }
    }
}