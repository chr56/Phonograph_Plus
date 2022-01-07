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
import player.phonograph.NOTIFICATION_CHANNEL_ID_DATABASE
import player.phonograph.NOTIFICATION_ID_DATABASE
import player.phonograph.R

object DatabaseUpdateNotification {
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
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_DATABASE)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentTitle(App.instance.getString(R.string.updating_database))
                    .setContentText(s)
                    .setProgress(0, 0, true)
                    .build()
            notificationManager.notify(NOTIFICATION_ID_DATABASE, notification)
            hasSent = true
            return NOTIFICATION_ID_DATABASE
        }
        return 0
    }

    fun cancelNotification() {
        notificationManager?.let {
            if (hasSent) it.cancel(NOTIFICATION_ID_DATABASE)
            hasSent = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {

        var notificationChannel: NotificationChannel? =
            notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_DATABASE)
        if (notificationChannel == null) {
            notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_DATABASE,
                App.instance.getString(R.string.database_update_notification_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description =
                App.instance.getString(R.string.database_update_notification_description)
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}
