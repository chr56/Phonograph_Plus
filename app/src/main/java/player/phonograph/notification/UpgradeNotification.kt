/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph.notification

import android.content.Context
import android.os.Bundle
import player.phonograph.App

object UpgradeNotification {
    private var impl: UpgradeNotificationImpl? = null
    private fun getImpl(context: Context): UpgradeNotificationImpl =
        impl ?: UpgradeNotificationImpl(context).also { impl = it }

    fun sendUpgradeNotification(versionInfo: Bundle, context: Context = App.instance) {
        getImpl(context).sendUpgradeNotification(context, versionInfo)
    }
}
