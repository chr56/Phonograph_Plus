/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.notification

import androidx.annotation.RequiresApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES

abstract class AbsNotificationImpl {

    protected abstract val channelId: String
    protected abstract val channelName: CharSequence
    protected abstract val importance: Int
    protected open val channelCfg: NotificationChannel.() -> Unit = {}

    protected inline fun execute(context: Context, block: NotificationManager.() -> Unit) {
        block(notificationManager(context))
    }

    protected fun notificationManager(context: Context): NotificationManager {
        val notificationManager =
            context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (SDK_INT >= VERSION_CODES.O) {
            checkNotificationChannel(notificationManager, channelId, channelName, importance, channelCfg)
        }
        return notificationManager
    }


    @RequiresApi(26)
    private fun checkNotificationChannel(
        notificationManager: NotificationManager,
        channelId: String,
        channelName: CharSequence,
        importance: Int,
        cfg: NotificationChannel.() -> Unit,
    ) {
        val notificationChannel: NotificationChannel? = notificationManager.getNotificationChannel(channelId)
        if (notificationChannel == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, channelName, importance)
                    .apply {
                        enableLights(false)
                        enableVibration(false)
                    }.apply(cfg)
            )
        }
    }

}