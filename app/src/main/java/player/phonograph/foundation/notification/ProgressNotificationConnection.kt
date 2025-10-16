/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.notification

import player.phonograph.R
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_DATABASE_SYNC
import player.phonograph.model.repo.sync.ProgressConnection
import android.content.Context

class ProgressNotificationConnection(
    val context: Context,
    titlePrefixRes: Int,
    channel: String = NOTIFICATION_CHANNEL_ID_DATABASE_SYNC,
) : ProgressConnection {

    private val notifications = Notifications.BackgroundTasks(channel)
    private val titlePrefix = context.getString(titlePrefixRes)
    private val defaultMessage = context.getString(R.string.state_updating)

    private fun title(message: String?): String =
        if (message != null)
            "$titlePrefix - $message"
        else
            "$titlePrefix - $defaultMessage"

    private var id = 101
    override fun onStart() {
        id = System.currentTimeMillis().mod(172_800_000)
    }

    override fun onProcessUpdate(message: String?) {
        if (message != null) notifications.post(
            context,
            title = title(message),
            msg = message,
            id = id,
            onGoing = true
        )
    }

    override fun onProcessUpdate(current: Int, total: Int) {
        notifications.post(
            context,
            title = title(null),
            msg = defaultMessage,
            id = id,
            process = current,
            maxProcess = total
        )
    }

    override fun onProcessUpdate(current: Int, total: Int, message: String?) {
        notifications.post(
            context,
            title = title(message),
            msg = message ?: defaultMessage,
            id = id,
            process = current,
            maxProcess = total
        )
    }

    override fun onCompleted() {
        notifications.cancel(context, id)
    }

    override fun onReset() {
        onCompleted()
        onStart()
    }
}