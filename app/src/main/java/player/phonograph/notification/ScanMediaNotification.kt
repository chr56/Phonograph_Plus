/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import player.phonograph.App
import player.phonograph.NOTIFICATION_CHANNEL_ID_SCAN_MEDIA
import player.phonograph.NOTIFICATION_ID_SCAN_MEDIA
import player.phonograph.R

object ScanMediaNotification {
    private var notificationManager: NotificationManager? = null
    private var isReady: Boolean = false
    private var hasSent: Boolean = false

    fun init() {
        notificationManager =
            App.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            createNotificationChannel(notificationManager!!)
        }
        isReady = true
    }
    fun sendNotification(s: String): Int {

        val context = App.instance
        if (!isReady) init()
        notificationManager?.let { notificationManager ->
            val notification: Notification =
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_SCAN_MEDIA)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentTitle(App.instance.getString(R.string.listing_files))
                    .setContentText(s)
                    .setProgress(0, 0, true)
                    .build()
            notificationManager.notify(NOTIFICATION_ID_SCAN_MEDIA, notification)
            hasSent = true
            return NOTIFICATION_ID_SCAN_MEDIA
        }
        return 0
    }

    fun cancelNotification() {
        notificationManager?.let {
            if (hasSent) it.cancel(NOTIFICATION_ID_SCAN_MEDIA)
            hasSent = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {

        var notificationChannel: NotificationChannel? =
            notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_SCAN_MEDIA)
        if (notificationChannel == null) {
            notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_SCAN_MEDIA,
                App.instance.getString(R.string.scan_media_notification_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description =
                App.instance.getString(R.string.scan_media_notification_description)
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationChannel.setSound(null, null)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}
