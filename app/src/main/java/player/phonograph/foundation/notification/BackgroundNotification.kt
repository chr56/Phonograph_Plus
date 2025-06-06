/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.notification

import android.content.Context

object BackgroundNotification {

    private var impl: BackgroundNotificationImpl? = null
    private fun getImpl(context: Context): BackgroundNotificationImpl =
        impl ?: BackgroundNotificationImpl(context).also { impl = it }

    /**
     * Post a common notification
     */
    fun post(context: Context, title: String, msg: String, id: Int, onGoing: Boolean = true) {
        getImpl(context).post(context, title, msg, id, onGoing)
    }

    /**
     * Post notification with process
     */
    fun post(context: Context, title: String, msg: String, id: Int, process: Int, maxProcess: Int) {
        getImpl(context).post(context, title, msg, id, process, maxProcess)
    }

    fun remove(context: Context, id: Int) {
        getImpl(context).remove(context, id)
    }
}
