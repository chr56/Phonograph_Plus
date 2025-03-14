/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.util

import player.phonograph.R
import player.phonograph.model.service.ACTION_CANCEL_PENDING_QUIT
import player.phonograph.model.service.ACTION_STOP_AND_QUIT_NOW
import player.phonograph.model.service.ACTION_STOP_AND_QUIT_PENDING
import player.phonograph.service.MusicService
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.reportError
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast

class SleepTimer private constructor() {

    private var currentTimerPendingIntent: PendingIntent? = null

    /**
     * @return true if the sleep timer was set
     */
    fun hasTimer() = currentTimerPendingIntent != null

    /**
     * Set a sleep timer
     * @param minutesToQuit minutes to stop Music Service
     * @param shouldFinishLastSong flag whether to complete current song then quit the Service on Sleep Timer's time-up
     */
    fun setTimer(context: Context, minutesToQuit: Long, shouldFinishLastSong: Boolean) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val nextSleepTimerElapsedTime = SystemClock.elapsedRealtime() + minutesToQuit * 60 * 1000
            Setting(context)[Keys.nextSleepTimerElapsedRealTime].data = nextSleepTimerElapsedTime

            val pendingIntent = stopMusicServicePendingIntent(context, shouldFinishLastSong)
            currentTimerPendingIntent = pendingIntent
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextSleepTimerElapsedTime, pendingIntent)

            Toast.makeText(
                context,
                context.getString(R.string.sleep_timer_set, minutesToQuit),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            reportError(e, TAG, "Failed to set sleep timer")
            Toast.makeText(
                context,
                context.getString(R.string.failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Cancel current timer
     */
    fun cancelTimer(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val pendingIntent = currentTimerPendingIntent
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                context.startService(cancelTimerIntent(context))
                pendingIntent.cancel()
                currentTimerPendingIntent = null
            }
            Toast.makeText(
                context,
                context.getString(R.string.sleep_timer_canceled),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            reportError(e, TAG, context.getString(R.string.failed))
            Toast.makeText(
                context,
                context.getString(R.string.failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        private var sleepTimer: SleepTimer? = null

        fun instance(): SleepTimer {
            return sleepTimer ?: SleepTimer().also { sleepTimer = it }
        }


        /**
         * PendingIntent to send a intent which to stop background MusicService
         * @param context Application context
         * @param shouldFinishLastSong flag whether to complete current song then quit the Service on Sleep Timer's time-up
         */
        private fun stopMusicServicePendingIntent(
            context: Context,
            shouldFinishLastSong: Boolean,
        ): PendingIntent =
            PendingIntent.getService(
                context,
                0,
                stopMusicServiceIntent(context.applicationContext, shouldFinishLastSong),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        /**
         * Intent to stop background MusicService
         * @param context Application context
         * @param shouldFinishLastSong flag whether to complete current song then quit the Service on Sleep Timer's time-up
         */
        private fun stopMusicServiceIntent(
            context: Context,
            shouldFinishLastSong: Boolean,
        ): Intent =
            Intent(context.applicationContext, MusicService::class.java).apply {
                action =
                    if (shouldFinishLastSong) ACTION_STOP_AND_QUIT_PENDING
                    else ACTION_STOP_AND_QUIT_NOW
            }


        private fun cancelTimerIntent(context: Context): Intent =
            Intent(context.applicationContext, MusicService::class.java).apply {
                action = ACTION_CANCEL_PENDING_QUIT
            }

        private const val TAG = "SleepTimer"
    }
}
