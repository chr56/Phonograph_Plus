/*
 * Copyright (c) 2022 chr_56
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
import player.phonograph.NOTIFICATION_CHANNEL_ID_BACKGROUND
import player.phonograph.R

object BackgroundNotification {
    private var notificationManager: NotificationManager? = null
    private var isReady: Boolean = false

    fun init() {
        notificationManager =
            App.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            createNotificationChannel(notificationManager!!)
        }
        isReady = true
    }

    fun post(title: String, msg: String, id: Int, onGoing: Boolean = true) {
        val context = App.instance
        if (!isReady) init()
        notificationManager?.let { notificationManager ->

            val notification: Notification =
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_BACKGROUND)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setOngoing(onGoing)
                    .build()
            notificationManager.notify(id, notification)
        }
    }

    fun remove(id: Int) {
        if (!isReady) init()
        notificationManager?.cancel(id)
    }

    @RequiresApi(26)
    private fun createNotificationChannel(notificationManager: NotificationManager) {

        var notificationChannel: NotificationChannel? =
            notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_BACKGROUND)
        if (notificationChannel == null) {
            notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_BACKGROUND,
                App.instance.getString(R.string.background_notification_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}
