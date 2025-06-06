/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.notification

import player.phonograph.R
import player.phonograph.model.CrashReport.Type
import android.app.Activity
import android.content.Context

object ErrorNotification {

    lateinit var crashActivity: Class<out Activity>

    private var impl: ErrorNotificationImpl? = null
    private fun getImpl(context: Context, crashActivity: Class<out Activity>): ErrorNotificationImpl =
        impl ?: ErrorNotificationImpl(context, crashActivity).also { impl = it }

    fun postErrorNotification(
        context: Context,
        exception: Throwable,
        note: String,
        @Type type: Int,
    ) {
        getImpl(context, crashActivity).send(
            context = context,
            title = "${exception::class.simpleName}",
            note = note,
            type = type,
            throwable = exception,
        )
    }

    fun postErrorNotification(
        context: Context,
        note: String,
        @Type type: Int,
    ) {
        getImpl(context, crashActivity).send(
            context = context,
            title = context.getString(R.string.title_internal_error),
            note = note,
            type = type,
        )
    }

}
