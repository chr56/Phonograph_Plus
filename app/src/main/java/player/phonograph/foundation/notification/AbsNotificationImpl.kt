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

    private var _notificationManager: NotificationManager? = null
    protected val notificationManager: NotificationManager get() = _notificationManager!!

    protected var isReady: Boolean = false
        private set

    protected abstract val channelId: String
    protected abstract val channelName: CharSequence
    protected abstract val importance: Int
    protected open val channelCfg: NotificationChannel.() -> Unit = {}

    protected fun init(context: Context) {
        _notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (SDK_INT >= VERSION_CODES.O && _notificationManager != null) {
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

}