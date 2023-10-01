/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.notification

import android.content.Context

object DatabaseUpdateNotification {

    private var impl: DatabaseUpdateNotificationImpl? = null
    private fun getImpl(context: Context): DatabaseUpdateNotificationImpl =
        impl ?: DatabaseUpdateNotificationImpl(context).also { impl = it }

    fun send(context: Context) {
        getImpl(context).sendNotification(context)
    }

    fun send(context: Context, text: String) {
        getImpl(context).sendNotification(context, text)
    }

    fun cancel(context: Context) {
        getImpl(context).cancelNotification(context)
    }
}