/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.notification

import android.app.Activity
import android.content.Context
import player.phonograph.App
import player.phonograph.R

object ErrorNotification {
    /**
     * Target Error Report Activity, which can handler extra [KEY_STACK_TRACE] in start intent
     */
    lateinit var crashActivity: Class<out Activity>

    private var impl: ErrorNotificationImpl? = null
    private fun getImpl(context: Context, crashActivity: Class<out Activity>): ErrorNotificationImpl =
        impl ?: ErrorNotificationImpl(context, crashActivity).also { impl = it }

    @JvmOverloads
    fun postErrorNotification(e: Throwable, note: String? = null, context: Context = App.instance) {
        getImpl(context, crashActivity).send(
            msg = "$note\n${e.stackTraceToString()}}",
            title = "${e::class.simpleName}\n$note",
            context = context
        )
    }

    @JvmOverloads
    fun postErrorNotification(note: String, context: Context = App.instance) {
        getImpl(context, crashActivity).send(
            msg = note,
            title = App.instance.getString(R.string.error_notification_name),
            context = context
        )
    }

    const val KEY_STACK_TRACE = "stack_trace"
}
