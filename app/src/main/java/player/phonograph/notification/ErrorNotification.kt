/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import player.phonograph.App
import player.phonograph.KEY_STACK_TRACE
import player.phonograph.NOTIFICATION_CHANNEL_ID_ERROR
import player.phonograph.R

object ErrorNotification {
    /**
     * Target Error Report Activity, which can handler extra [KEY_STACK_TRACE] in start intent
     */
    lateinit var crashActivity: Class<out Activity>

    private var notificationManager: NotificationManager? = null
    private var isReady: Boolean = false

    private var count = 0

    fun init() {
        notificationManager =
            App.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            createNotificationChannel(notificationManager!!)
        }
        isReady = true
    }

    fun postErrorNotification(e: Throwable, note: String? = null, context: Context = App.instance) {
        send(msg = "$note\n${e::class.simpleName}\n${e.message}", title = "${e::class.simpleName}\n$note", context = context)
    }
    fun postErrorNotification(note: String, context: Context = App.instance) {
        send(msg = note, title = App.instance.getString(R.string.error_notification_name), context = context)
    }

    private fun send(msg: String, title: String? = null, context: Context = App.instance) {
        if (!isReady) init()

        val action = Intent(context, crashActivity).apply {
            putExtra(KEY_STACK_TRACE, msg)
        }

        val clickIntent: PendingIntent = PendingIntent.getActivity(context, 0, action, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        notificationManager?.let { notificationManager ->
            val notification: Notification =
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_ERROR)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setCategory(NotificationCompat.CATEGORY_ERROR)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentTitle(context.getString(R.string.error_notification_name))
                    .setContentText(msg)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .setBigContentTitle(context.getString(R.string.error_notification_name))
                            .setSummaryText(title)
                            .bigText(msg)
                    )
                    .setContentIntent(clickIntent)
                    .setAutoCancel(true)
                    .build()

            count++
            notificationManager.notify(count, notification)
        }
    }

    @RequiresApi(26)
    private fun createNotificationChannel(notificationManager: NotificationManager) {

        var notificationChannel: NotificationChannel? =
            notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_ERROR)
        if (notificationChannel == null) {
            notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_ERROR,
                App.instance.getString(R.string.error_notification_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}
