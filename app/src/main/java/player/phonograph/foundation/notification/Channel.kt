/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.notification

import player.phonograph.R
import player.phonograph.model.notification.ChannelID
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_BACKGROUND
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_DEFAULT
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_ERROR
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_UPGRADE
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build.VERSION_CODES

enum class Channel(
    @get:ChannelID val id: String,
    @get:StringRes val nameResources: Int,
    @get:StringRes val descriptionResources: Int = 0,
    val importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
    val showBadge: Boolean = false,
    val lights: Boolean = false,
    val vibration: Boolean = false,
) {
    DEFAULT(
        NOTIFICATION_CHANNEL_ID_DEFAULT,
        nameResources = R.string.pref_header_notification,
    ),
    ERROR(
        NOTIFICATION_CHANNEL_ID_ERROR,
        nameResources = R.string.notification_error_name,
        importance = NotificationManager.IMPORTANCE_HIGH,
    ),
    UPGRADE(
        NOTIFICATION_CHANNEL_ID_UPGRADE,
        nameResources = R.string.notification_update_name,
        descriptionResources = R.string.notification_update_description,
        importance = NotificationManager.IMPORTANCE_HIGH,
    ),
    BACKGROUND(
        NOTIFICATION_CHANNEL_ID_BACKGROUND,
        nameResources = R.string.notification_background_name,
        importance = NotificationManager.IMPORTANCE_HIGH,
    ),
    ;

    fun name(context: Context) = context.getString(nameResources)
    fun description(context: Context) = context.getString(descriptionResources)


    @RequiresApi(VERSION_CODES.O)
    fun asNotificationChannel(context: Context): NotificationChannel =
        NotificationChannel(id, name(context), importance).apply {
            if (descriptionResources > 0) description = description(context)
            enableLights(lights)
            enableVibration(vibration)
            setShowBadge(showBadge)
        }

    @RequiresApi(VERSION_CODES.O)
    fun check(context: Context, notificationManager: NotificationManagerCompat) {
        val notificationChannel: NotificationChannel? = notificationManager.getNotificationChannel(id)
        if (notificationChannel == null) {
            notificationManager.createNotificationChannel(asNotificationChannel(context))
        }
    }

    companion object {

        @JvmStatic
        fun id(channelId: String): Channel? {
            val channel = when (channelId) {
                NOTIFICATION_CHANNEL_ID_ERROR      -> ERROR
                NOTIFICATION_CHANNEL_ID_UPGRADE    -> UPGRADE
                NOTIFICATION_CHANNEL_ID_BACKGROUND -> BACKGROUND
                NOTIFICATION_CHANNEL_ID_DEFAULT    -> DEFAULT
                else                               -> null
            }
            return channel
        }

        @JvmStatic
        @RequiresApi(VERSION_CODES.O)
        fun validExistence(
            context: Context,
            notificationManager: NotificationManagerCompat,
            channelId: String,
        ): String? {
            val channel = id(channelId)
            return if (channel != null) {
                channel.check(context, notificationManager)
                channelId
            } else {
                null
            }
        }
    }
}