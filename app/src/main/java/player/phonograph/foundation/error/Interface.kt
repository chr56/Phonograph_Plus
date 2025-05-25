/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.error

import player.phonograph.model.CrashReport
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log

fun startCrashActivity(context: Context, throwable: Throwable, crashActivity: Class<out Activity>) {

    val report =
        if (throwable is InternalDataCorruptedException) {
            val cause = throwable.cause
            val description = cause?.let { ExceptionDescriptions.description(it) }
            CrashReport(
                type = CrashReport.CRASH_TYPE_CORRUPTED_DATA,
                note = "Internal data is corrupted, try to wipe application data! $description",
                stackTrace = Log.getStackTraceString(cause),
            )
        } else
            CrashReport(
                type = CrashReport.CRASH_TYPE_CRASH,
                note = "Application crashed and exited unexpectedly!",
                stackTrace = Log.getStackTraceString(throwable),
            )
    context.startActivity(
        Intent(context, crashActivity)
            .apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(CrashReport.KEY, report)
            }
    )
}

class InternalDataCorruptedException(message: String, cause: Throwable) : Exception(message, cause)