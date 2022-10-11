/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import player.phonograph.R
import player.phonograph.VERSION_INFO
import player.phonograph.misc.VersionJson
import player.phonograph.ui.activities.MainActivity
import player.phonograph.ui.activities.MainActivity.Companion.SHOW_UPGRADE_DIALOG

class UpgradeNotificationImpl(context: Context) : AbsNotificationImpl() {

    override val channelId: String = NOTIFICATION_CHANNEL_ID_UPGRADABLE
    override val channelName: CharSequence = context.getString(R.string.upgrade_notification_name)
    override val importance: Int = NotificationManager.IMPORTANCE_HIGH

    fun sendUpgradeNotification(context: Context, versionInfo: Bundle) {
        execute(context) {
            val action = Intent(context, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                this.putExtra(SHOW_UPGRADE_DIALOG, true)
                this.putExtra(VERSION_INFO, versionInfo)
            }
            val clickIntent: PendingIntent =
                PendingIntent.getActivity(context, 0, action, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val notification: Notification =
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_UPGRADABLE)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentTitle(context.getText(R.string.new_version))
                    .setContentText(
                        "${context.getText(R.string.new_version_code)} ${versionInfo.getString(VersionJson.VERSION)}"
                    )
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .setBigContentTitle("${context.getText(R.string.new_version_code)} ${versionInfo.getString(VersionJson.VERSION)}")
//                            .bigText("${context.getString(R.string.new_version_log)} ${versionInfo.getString(VersionJson.LOG_SUMMARY) ?: "NULL"}")
                    )
                    .setContentIntent(clickIntent)
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .build()
            notificationManager.notify(NOTIFICATION_ID_UPGRADABLE, notification)
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID_UPGRADABLE = "upgrade_notification"
        private const val NOTIFICATION_ID_UPGRADABLE = 8747233
    }

}
