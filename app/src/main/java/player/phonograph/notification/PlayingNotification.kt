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
        private const val NOTIFY_MODE_FOREGROUND = 1
        private const val NOTIFY_MODE_BACKGROUND = 0
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

    private var notifyMode = NOTIFY_MODE_BACKGROUND
    abstract fun update()

    @JvmField
    protected var stopped = false

    @Synchronized
    fun stop() {
        stopped = true
        service.stopForeground(true)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    protected fun updateNotifyModeAndPostNotification(notification: Notification?) {
        val newNotifyMode: Int = if (service.isPlaying) NOTIFY_MODE_FOREGROUND else NOTIFY_MODE_BACKGROUND

        if (notifyMode != newNotifyMode && newNotifyMode == NOTIFY_MODE_BACKGROUND) {
            service.stopForeground(false)
        }
        when (newNotifyMode) {
            NOTIFY_MODE_FOREGROUND -> {
                service.startForeground(NOTIFICATION_ID, notification)
            }
            NOTIFY_MODE_BACKGROUND -> {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
        notifyMode = newNotifyMode
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
