/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.notification

import player.phonograph.R
import player.phonograph.model.notification.ChannelID
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_APP_UPDATE
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_BACKGROUND_TASKS
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_DATABASE_SYNC
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_DEFAULT
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_ERROR_REPORT
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.util.Log


sealed class Notifications {

    protected fun post(
        context: Context,
        @ChannelID channelId: String,
        id: Int, tag: String? = null,
        block: NotificationCompat.Builder.() -> Unit,
    ) {

        if (SDK_INT >= VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context.applicationContext, POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "No post notification permission")
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)

        val actualChannelId =
            if (SDK_INT >= VERSION_CODES.O) {
                Channel.validExistence(context, notificationManager, channelId) ?: run {
                    Log.w(TAG, "No such Notification Channel: $channelId")
                    NOTIFICATION_CHANNEL_ID_DEFAULT
                }
            } else {
                NOTIFICATION_CHANNEL_ID_DEFAULT
            }

        notificationManager.notify(
            tag,
            id,
            NotificationCompat.Builder(context.applicationContext, actualChannelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .apply(block).build()
        )
    }

    protected fun buildClickIntent(
        context: Context,
        handlerIntent: Intent,
        requestCode: Int = 0,
        flags: Int = FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT,
    ): PendingIntent = PendingIntent.getActivity(context, requestCode, handlerIntent, flags)

    fun cancel(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    class BackgroundTasks(private val channel: String) : Notifications() {
        companion object {
            val Database get() = BackgroundTasks(NOTIFICATION_CHANNEL_ID_DATABASE_SYNC)
            val Default get() = BackgroundTasks(NOTIFICATION_CHANNEL_ID_BACKGROUND_TASKS)
        }

        fun post(context: Context, title: String, msg: String, id: Int, onGoing: Boolean = true) =
            post(context, channel, id) {
                setCategory(NotificationCompat.CATEGORY_PROGRESS)
                setPriority(NotificationCompat.PRIORITY_DEFAULT)
                setContentTitle(title)
                setContentText(msg)
                setOngoing(onGoing)
            }

        fun post(context: Context, title: String, msg: String, id: Int, process: Int, maxProcess: Int) =
            post(context, channel, id) {
                setCategory(NotificationCompat.CATEGORY_PROGRESS)
                setPriority(NotificationCompat.PRIORITY_DEFAULT)
                setContentTitle(title)
                setContentText(msg)
                setOngoing(true)
                setProgress(maxProcess, process, false)
            }
    }

    object Error : Notifications() {
        private const val TARGET_CHANNEL = NOTIFICATION_CHANNEL_ID_ERROR_REPORT
        fun post(
            context: Context,
            title: String,
            note: String,
            handlerIntent: Intent,
        ) {
            val clickIntent: PendingIntent = buildClickIntent(context, handlerIntent)
            post(context, TARGET_CHANNEL, count) {
                setCategory(NotificationCompat.CATEGORY_ERROR)
                setPriority(NotificationCompat.PRIORITY_HIGH)
                setContentIntent(clickIntent)
                setAutoCancel(true)
                setContentTitle(title)
                setContentText(note)
                setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(context.getString(R.string.notification_error_name))
                        .setSummaryText(title).bigText(note)
                )
            }
            count = (count + 1) % 1001
        }

        private var count = 101
    }

    object Upgrade : Notifications() {
        private const val NOTIFICATION_ID_UPGRADABLE = 8747233
        private const val TARGET_CHANNEL = NOTIFICATION_CHANNEL_ID_APP_UPDATE

        fun post(
            context: Context,
            title: String,
            note: CharSequence,
            handlerIntent: Intent,
        ) {
            val clickIntent: PendingIntent = buildClickIntent(context, handlerIntent)
            post(context, TARGET_CHANNEL, NOTIFICATION_ID_UPGRADABLE) {
                setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                setPriority(NotificationCompat.PRIORITY_HIGH)
                setContentTitle(context.getText(R.string.msg_new_version_available))
                setContentText("$title\n$note")
                setStyle(
                    NotificationCompat.BigTextStyle()
                        .setSummaryText(note)
                        .setBigContentTitle(title)
                )
                setContentIntent(clickIntent)
                setAutoCancel(true)
                setOngoing(false)
            }
        }
    }

    companion object {
        private const val TAG = "Notifications"
    }
}



