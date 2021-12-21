/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import player.phonograph.*
import player.phonograph.ui.activities.MainActivity

object UpgradeNotification {
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

    fun sendUpgradeNotification(versionInfo: Bundle) {
        val context = App.instance
        if (!isReady) init()

        notificationManager?.let { notificationManager ->

            val action = Intent(context, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                this.putExtra(UPGRADABLE, true)
                this.putExtra(VERSION_INFO, versionInfo)
            }
            val clickIntent: PendingIntent = PendingIntent.getActivity(context, 0, action, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val notification: Notification =
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_UPGRADABLE)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentTitle(context.getText(R.string.new_version))
                    .setContentText(
                        "${context.getText(R.string.new_version_code)} ${versionInfo.getString(Updater.Version)}"
                    )
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .setBigContentTitle("${context.getText(R.string.new_version_code)} ${versionInfo.getString(Updater.Version)}")
                            .bigText("${context.getString(R.string.new_version_log)} ${versionInfo.getString(Updater.LogSummary) ?: "NULL"}")
                    )
                    .setContentIntent(clickIntent)
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .build()
            notificationManager.notify(NOTIFICATION_ID_UPGRADABLE, notification)
        }
    }

    @RequiresApi(26)
    private fun createNotificationChannel(notificationManager: NotificationManager) {

        var notificationChannel: NotificationChannel? =
            notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_UPGRADABLE)
        if (notificationChannel == null) {
            notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_UPGRADABLE,
                App.instance.getString(R.string.upgrade_notification_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description =
                App.instance.getString(R.string.upgrade_notification_description)
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}
