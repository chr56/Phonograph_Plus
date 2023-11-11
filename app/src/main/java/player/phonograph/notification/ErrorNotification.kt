/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.notification

import player.phonograph.App
import player.phonograph.R
import android.app.Activity
import android.content.Context

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
            note = note ?: e.message ?: "",
            throwable = e,
            title = "${e::class.simpleName}",
            context = context
        )
    }

    @JvmOverloads
    fun postErrorNotification(note: String, context: Context = App.instance) {
        getImpl(context, crashActivity).send(
            note = note,
            title = App.instance.getString(R.string.internal_error),
            context = context
        )
    }

    const val KEY_NOTE = "note"
    const val KEY_STACK_TRACE = "stack_trace"
    const val KEY_IS_A_CRASH = "is_a_crash"
}
