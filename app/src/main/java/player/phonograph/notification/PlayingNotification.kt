package player.phonograph.notification

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
import player.phonograph.model.Song
import player.phonograph.service.MusicService
import player.phonograph.ui.activities.MainActivity
import android.app.Notification as OSNotification
import androidx.core.app.NotificationCompat as XNotificationCompat

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

    protected var notificationBuilder: XNotificationCompat.Builder

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(service: MusicService) {
        _service = service
        notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationBuilder = XNotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(clickPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setPriority(XNotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_SERVICE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel()
    }

    private val currentMode
        get() = if (service.isPlaying) MODE_FOREGROUND else MODE_BACKGROUND
    private val availability
        get() = !service.isIdle

    protected abstract fun update()

    var metaData: SongMetaData? = null
        set(value) {
            field = value
            update()
        }

    @Synchronized
    fun stop() {
        service.stopForeground(true)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    protected fun updateNotification(notification: OSNotification) {
        if (!availability) {
            // service available/stopped
            stop()
            return
        }
        when (currentMode) {
            MODE_FOREGROUND -> {
                notificationManager.notify(NOTIFICATION_ID, notification)
                service.startForeground(NOTIFICATION_ID, notification)
            }
            MODE_BACKGROUND -> {
                notificationManager.notify(NOTIFICATION_ID, notification)
                service.stopForeground(false)
            }
        }
    }

    @RequiresApi(26)
    private fun createNotificationChannel() {
        notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID).also { exist ->
            if (exist == null)
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        App.instance.getString(R.string.playing_notification_name),
                        NotificationManager.IMPORTANCE_LOW
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

    /**
     * PendingIntent to launch MainActivity
     */
    protected val clickPendingIntent: PendingIntent get() = PendingIntent.getActivity(
        service, 0,
        Intent(service, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
    )

    /**
     * PendingIntent to quit/stop
     */
    val deletePendingIntent get() = buildPlaybackPendingIntent(MusicService.ACTION_QUIT)

    data class SongMetaData(val song: Song)
}
