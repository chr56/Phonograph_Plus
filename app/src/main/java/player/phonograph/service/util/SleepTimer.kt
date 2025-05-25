/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.util

import player.phonograph.R
import player.phonograph.foundation.error.warning
import player.phonograph.model.service.ACTION_CANCEL_PENDING_QUIT
import player.phonograph.model.service.ACTION_STOP_AND_QUIT_NOW
import player.phonograph.model.service.ACTION_STOP_AND_QUIT_PENDING
import player.phonograph.service.MusicService
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Sleep timer, for one timer instance
 */
object SleepTimer {

    private var nextSleepTimerElapsedTime: Long = -1L
    private var currentTimerPendingIntent: PendingIntent? = null
    private var pendingQuit = false

    /**
     * @return next sleep timer elapsed time
     */
    fun timerElapsedTime() = nextSleepTimerElapsedTime

    /**
     * @return true if the sleep timer was set
     */
    fun hasTimer() = currentTimerPendingIntent != null && nextSleepTimerElapsedTime > SystemClock.elapsedRealtime()


    /**
     * Set a sleep timer
     * @param minutesToQuit minutes to stop Music Service
     * @param shouldFinishLastSong flag whether to complete current song then quit the Service on Sleep Timer's time-up
     */
    fun setTimer(context: Context, minutesToQuit: Long, shouldFinishLastSong: Boolean): Boolean = try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        synchronized(this) {
            // Cancel previous one
            val previousPendingIntent = currentTimerPendingIntent
            if (previousPendingIntent != null) {
                alarmManager.cancel(previousPendingIntent)
                previousPendingIntent.cancel()
                if (pendingQuit) {
                    context.startService(cancelPendingQuitIntent(context))
                }
            }

            // Make new one
            pendingQuit = shouldFinishLastSong
            nextSleepTimerElapsedTime = SystemClock.elapsedRealtime() + minutesToQuit * 60 * 1000
            val pendingIntent = stopMusicServicePendingIntent(context, shouldFinishLastSong).also {
                currentTimerPendingIntent = it
            }
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextSleepTimerElapsedTime, pendingIntent)
        }
        true
    } catch (e: Exception) {
        warning(context, TAG, "Failed to set sleep timer", e)
        false
    }

    /**
     * Cancel current timer
     */
    fun cancelTimer(context: Context): Boolean = try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        synchronized(this) {
            val pendingIntent = currentTimerPendingIntent
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                if (pendingQuit) {
                    context.startService(cancelPendingQuitIntent(context))
                }
                currentTimerPendingIntent = null
                nextSleepTimerElapsedTime = -1L
            }
        }
        true
    } catch (e: Exception) {
        warning(context, TAG, context.getString(R.string.failed), e)
        false
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
            context.applicationContext,
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
            action = if (shouldFinishLastSong) ACTION_STOP_AND_QUIT_PENDING else ACTION_STOP_AND_QUIT_NOW
        }


    /**
     * Intent to cancel `shouldFinishLastSong` (pending quit) settings for service
     */
    private fun cancelPendingQuitIntent(context: Context): Intent =
        Intent(context.applicationContext, MusicService::class.java).apply {
            action = ACTION_CANCEL_PENDING_QUIT
        }

    private const val TAG = "SleepTimer"
}
