/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.notification

import player.phonograph.R
import androidx.core.app.NotificationCompat
import android.app.Notification
import android.app.NotificationManager
import android.content.Context

class BackgroundNotificationImpl(context: Context) : AbsNotificationImpl() {

    override val channelId = NOTIFICATION_CHANNEL_ID_BACKGROUND
    override val channelName = context.getString(R.string.notification_background_name)
    override val importance = NotificationManager.IMPORTANCE_HIGH


    fun post(context: Context, title: String, msg: String, id: Int, onGoing: Boolean = true) =
        execute(context) {
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
            notify(id, notification)
        }

    fun post(context: Context, title: String, msg: String, id: Int, process: Int, maxProcess: Int) =
        execute(context) {
            val notification: Notification =
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_BACKGROUND)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentTitle(title)
                    .setContentText(msg)
                    .setOngoing(true)
                    .setProgress(maxProcess, process, false)
                    .build()
            notify(id, notification)
        }

    fun remove(context: Context, id: Int) {
        execute(context) {
            cancel(id)
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID_BACKGROUND = "background_notification"
    }
}