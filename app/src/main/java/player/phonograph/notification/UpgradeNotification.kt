/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph.notification

import android.content.Context
import player.phonograph.App
import player.phonograph.model.version.VersionCatalog

object UpgradeNotification {
    private var impl: UpgradeNotificationImpl? = null
    private fun getImpl(context: Context): UpgradeNotificationImpl =
        impl ?: UpgradeNotificationImpl(context).also { impl = it }

    fun sendUpgradeNotification(versionCatalog: VersionCatalog, channel: String, context: Context = App.instance) {
        getImpl(context).sendUpgradeNotification(context, versionCatalog, channel)
    }
}
