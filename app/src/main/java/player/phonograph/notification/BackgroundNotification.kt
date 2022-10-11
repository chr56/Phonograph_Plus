/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.notification

import android.content.Context
import player.phonograph.App

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
