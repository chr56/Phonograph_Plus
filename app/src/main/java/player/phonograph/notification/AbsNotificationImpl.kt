/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

abstract class AbsNotificationImpl {

    protected val notificationManager: NotificationManager get() = sNotificationManager!!

    protected var isReady: Boolean = false
        private set

    protected abstract val channelId: String
    protected abstract val channelName: CharSequence
    protected abstract val importance: Int
    protected open val channelCfg: NotificationChannel.() -> Unit = {}

    protected fun init(context: Context) {
        sNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && sNotificationManager != null) {
            createNotificationChannel(notificationManager, channelId, channelName, importance, channelCfg)
        }
        isReady = true
    }

    protected inline fun execute(context: Context, block: NotificationManager.() -> Unit) {
        if (!isReady) init(context)
        block(notificationManager)
    }


    @RequiresApi(26)
    private fun createNotificationChannel(
        notificationManager: NotificationManager,
        channelId: String,
        channelName: CharSequence,
        importance: Int,
        cfg: NotificationChannel.() -> Unit,
    ) {
        val notificationChannel: NotificationChannel? = notificationManager.getNotificationChannel(channelId)
        if (notificationChannel == null) {
            notificationManager.createNotificationChannel(NotificationChannel(channelId, channelName, importance)
                .apply {
                    enableLights(false)
                    enableVibration(false)
                }.apply(cfg)
            )
        }
    }

    companion object {
        private var sNotificationManager: NotificationManager? = null
    }

}