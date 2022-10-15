/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph.notification

import android.content.Context
import android.os.Bundle
import player.phonograph.App
import player.phonograph.model.version.VersionCatalog

object UpgradeNotification2 {
    private var impl: UpgradeNotificationImpl2? = null
    private fun getImpl(context: Context): UpgradeNotificationImpl2 =
        impl ?: UpgradeNotificationImpl2(context).also { impl = it }

    fun sendUpgradeNotification(versionCatalog: VersionCatalog, channel: String, context: Context = App.instance) {
        getImpl(context).sendUpgradeNotification(context, versionCatalog, channel)
    }
}
