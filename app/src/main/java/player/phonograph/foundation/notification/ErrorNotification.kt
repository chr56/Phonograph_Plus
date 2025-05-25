/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.notification

import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.CrashReport
import player.phonograph.model.CrashReport.Type
import android.app.Activity
import android.content.Context

object ErrorNotification {

    lateinit var crashActivity: Class<out Activity>

    private var impl: ErrorNotificationImpl? = null
    private fun getImpl(context: Context, crashActivity: Class<out Activity>): ErrorNotificationImpl =
        impl ?: ErrorNotificationImpl(context, crashActivity).also { impl = it }

    @JvmOverloads
    fun postErrorNotification(
        exception: Throwable,
        note: String? = null,
        @Type type: Int = CrashReport.CRASH_TYPE_INTERNAL_ERROR,
        context: Context = App.instance,
    ) {
        getImpl(context, crashActivity).send(
            context = context,
            title = "${exception::class.simpleName}",
            note = note ?: exception.message ?: "",
            type = type,
            throwable = exception,
        )
    }

    @JvmOverloads
    fun postErrorNotification(
        note: String,
        @Type type: Int,
        context: Context = App.instance,
    ) {
        getImpl(context, crashActivity).send(
            context = context,
            title = App.instance.getString(R.string.title_internal_error),
            note = note,
            type = type,
        )
    }

}
