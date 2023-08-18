/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.notification

import player.phonograph.App
import player.phonograph.R
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.Notification
import android.content.Context

class DatabaseUpdateNotificationImpl(context: Context) : AbsNotificationImpl() {
    override val channelId: String
        get() = NOTIFICATION_CHANNEL_DATABASE_UPDATE
    override val channelName: CharSequence
        get() = NOTIFICATION_CHANNEL_DATABASE_UPDATE //todo
    override val importance: Int
        get() = NotificationManagerCompat.IMPORTANCE_DEFAULT

    fun sendNotification(context: Context) {
        sendNotification(context, context.getString(R.string.updating_database))
    }

    fun sendNotification(context: Context, text: String) {
        execute(context) {
            val notification: Notification =
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_DATABASE_UPDATE)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentTitle(App.instance.getString(R.string.updating_database))
                    .setContentText(text)
                    .setProgress(0, 0, true)
                    .build()
            notify(NOTIFICATION_CHANNEL_ID_DATABASE_UPDATE, notification)
        }
    }

    fun cancelNotification(context: Context) {
        execute(context){
            cancel(NOTIFICATION_CHANNEL_ID_DATABASE_UPDATE)
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_DATABASE_UPDATE = "update_database"
        private const val NOTIFICATION_CHANNEL_ID_DATABASE_UPDATE = 0
    }
}