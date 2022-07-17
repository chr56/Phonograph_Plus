/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import java.lang.ref.WeakReference
import player.phonograph.App
import player.phonograph.service.MusicService
import player.phonograph.settings.Setting

class SleepTimer private constructor(s: MusicService) {
    val reference: WeakReference<MusicService> = WeakReference(s)

    var currentTimerPendingIntent: PendingIntent? = null
        private set

    /**
     * true if the sleep timer was set
     */
    fun hasTimer() = currentTimerPendingIntent != null

    internal val alarmManager = s.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Set a sleep timer
     * @param minutesToQuit minutes to stop Music Service
     * @param shouldFinishLastSong flag whether to complete current song then quit the Service on Sleep Timer's time-up
     */
    fun setTimer(minutesToQuit: Long, shouldFinishLastSong: Boolean): Boolean {
        val context: Context = reference.get() ?: return false
        return runCatching {
            val nextSleepTimerElapsedTime = SystemClock.elapsedRealtime() + minutesToQuit * 60 * 1000
            Setting.instance.nextSleepTimerElapsedRealTime = nextSleepTimerElapsedTime

            currentTimerPendingIntent = stopMusicServicePendingIntent(context, shouldFinishLastSong)
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                nextSleepTimerElapsedTime,
                currentTimerPendingIntent!!
            )
        }.isSuccess
    }

    /**
     * Cancel current timer
     */
    fun cancelTimer(): Boolean {
        val context: Context = reference.get() ?: return false
        return runCatching {
            currentTimerPendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                context.startService(
                    cancelTimerIntent()
                )
            }
            currentTimerPendingIntent = null
        }.isSuccess
    }

    companion object {
        private var s: SleepTimer? = null

        /**
         * @param musicService the MusicService to create the instance if static field is null
         */
        fun instance(musicService: MusicService): SleepTimer {
            return s ?: SleepTimer(musicService).also { s = it }
        }

        /**
         * Intent to stop background MusicService
         * @param context Application context
         * @param shouldFinishLastSong flag whether to complete current song then quit the Service on Sleep Timer's time-up
         */
        private fun stopMusicServiceIntent(
            context: Context = App.instance,
            shouldFinishLastSong: Boolean
        ): Intent =
            Intent(context, MusicService::class.java).apply {
                action = if (shouldFinishLastSong) {
                    MusicService.ACTION_STOP_AND_QUIT_PENDING
                } else MusicService.ACTION_STOP_AND_QUIT_NOW
            }

        /**
         * PendingIntent to send a intent which to stop background MusicService
         * @param context Application context
         * @param shouldFinishLastSong flag whether to complete current song then quit the Service on Sleep Timer's time-up
         */
        private fun stopMusicServicePendingIntent(
            context: Context,
            shouldFinishLastSong: Boolean
        ): PendingIntent? =
            PendingIntent.getService(
                context,
                0,
                stopMusicServiceIntent(context.applicationContext, shouldFinishLastSong),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        private fun cancelTimerIntent(context: Context = App.instance): Intent = Intent(
            context.applicationContext,
            MusicService::class.java
        ).apply {
            action = MusicService.ACTION_CANCEL_PENDING_QUIT
        }
    }
}
