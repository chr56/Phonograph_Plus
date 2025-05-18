/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.notification

import player.phonograph.App
import player.phonograph.model.version.ReleaseChannel
import player.phonograph.model.version.VersionCatalog
import android.content.Context

object UpgradeNotification {
    private var impl: UpgradeNotificationImpl? = null
    private fun getImpl(context: Context): UpgradeNotificationImpl =
        impl ?: UpgradeNotificationImpl(context).also { impl = it }

    fun sendUpgradeNotification(versionCatalog: VersionCatalog, channel: ReleaseChannel, context: Context = App.instance) {
        getImpl(context).sendUpgradeNotification(context, versionCatalog, channel)
    }
}
