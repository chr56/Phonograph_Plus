/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.notification

import player.phonograph.model.version.ReleaseChannel
import player.phonograph.model.version.VersionCatalog
import android.content.Context
import android.content.Intent

object UpgradeNotification {
    private var impl: UpgradeNotificationImpl? = null
    private fun getImpl(context: Context): UpgradeNotificationImpl =
        impl ?: UpgradeNotificationImpl(context).also { impl = it }

    fun sendUpgradeNotification(
        context: Context,
        versionCatalog: VersionCatalog,
        channel: ReleaseChannel,
        handlerIntent: Intent,
    ) {
        getImpl(context).sendUpgradeNotification(context, versionCatalog, channel, handlerIntent)
    }
}
