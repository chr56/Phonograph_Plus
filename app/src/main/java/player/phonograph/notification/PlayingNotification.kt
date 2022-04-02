package player.phonograph.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import player.phonograph.App
import player.phonograph.R
import player.phonograph.service.MusicService
import kotlin.jvm.Synchronized

abstract class PlayingNotification {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "playing_notification"
        private const val NOTIFICATION_ID = 1
        private const val MODE_FOREGROUND = 1
        private const val MODE_BACKGROUND = 0
    }

    private var notificationManager: NotificationManager

    private var _service: MusicService? = null
    protected val service: MusicService get() = _service!!

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(service: MusicService) {
        _service = service
        notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel()
    }

    private val currentMode
        get() = if (service.isPlaying) MODE_FOREGROUND else MODE_BACKGROUND
    private val availability
        get() = !service.isIdle
    abstract fun update()

    @Synchronized
    fun stop() {
        service.stopForeground(true)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    protected fun updateNotification(notification: Notification) {
        if (!availability){
            // service available/stopped
            stop()
            return
        }
        when (currentMode) {
            MODE_FOREGROUND -> {
                // service.stopForeground(false)
                notificationManager.notify(NOTIFICATION_ID, notification)
                service.startForeground(NOTIFICATION_ID, notification)
            }
            MODE_BACKGROUND -> {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    @RequiresApi(26)
    private fun createNotificationChannel() {
        notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID).also { exist ->
            if (exist == null)
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID, App.instance.getString(R.string.playing_notification_name), NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = App.instance.getString(R.string.playing_notification_description)
                        enableLights(false)
                        enableVibration(false)
                    }
                )
        }
    }

    /**
     * PendingIntent for Playback control buttons
     * @param action actions in [MusicService]
     * @return PendingIntent to operation action
     */
    protected fun buildPlaybackPendingIntent(action: String): PendingIntent =
        PendingIntent.getService(
            service, 0,
            Intent(action).apply { component = ComponentName(service, MusicService::class.java) },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
}
