/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.notification

import player.phonograph.App
import android.content.Context

object BackgroundNotification {

    private var impl: BackgroundNotificationImpl? = null
    private fun getImpl(context: Context): BackgroundNotificationImpl =
        impl ?: BackgroundNotificationImpl(context).also { impl = it }

    /**
     * Post a common notification
     */
    fun post(title: String, msg: String, id: Int, onGoing: Boolean = true, context: Context = App.instance) {
        getImpl(context).post(context, title, msg, id, onGoing)
    }

    /**
     * Post notification with process
     */
    fun post(title: String, msg: String, id: Int, process: Int, maxProcess: Int, context: Context = App.instance) {
        getImpl(context).post(context, title, msg, id, process, maxProcess)
    }

    fun remove(id: Int, context: Context = App.instance) {
        getImpl(context).remove(context, id)
    }
}
