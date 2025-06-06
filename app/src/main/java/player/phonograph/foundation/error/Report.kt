/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.error

import player.phonograph.foundation.notification.ErrorNotification
import player.phonograph.model.CrashReport
import player.phonograph.model.CrashReport.Type
import android.content.Context
import android.util.Log
import java.io.File


fun warning(
    context: Context,
    tag: String, message: String,
    throwable: Throwable? = null,
    @Type type: Int = CrashReport.CRASH_TYPE_INTERNAL_ERROR,
) {
    if (throwable == null) {
        Log.w(tag, message)
        ErrorNotification.postErrorNotification(context, message, type)
    } else {
        Log.e(tag, message, throwable)
        ErrorNotification.postErrorNotification(context, throwable, message, type)
    }
}

fun record(
    context: Context,
    throwable: Throwable,
    tag: String,
) {
    val report = ExceptionDescriptions.report(throwable)
    Log.w(tag, report)
    errorStacksFile(context.applicationContext)?.appendText(
        "----------------\nError at timestamp(${System.currentTimeMillis()}):\n$report"
    )
}

private const val ERROR_STACKS_FILE_NAME = "errors.txt"
private fun errorStacksFile(context: Context): File? {
    val externalCacheDir = context.externalCacheDir
    return if (externalCacheDir != null && externalCacheDir.isDirectory) {
        val file = File(externalCacheDir, ERROR_STACKS_FILE_NAME)
        if (file.exists() && !file.isFile) {
            file.delete()
            file.createNewFile()
        } else {
            file.createNewFile()
        }
        file
    } else {
        null
    }
}


