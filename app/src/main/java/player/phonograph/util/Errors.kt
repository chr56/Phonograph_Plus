/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util

import player.phonograph.model.CrashReport
import player.phonograph.model.CrashReport.Type
import player.phonograph.notification.ErrorNotification
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.File
import java.io.StringWriter

private const val ERROR_STACKS_FILE = "errors.txt"

fun reportError(
    e: Throwable,
    tag: String,
    message: String,
    @Type type: Int = CrashReport.CRASH_TYPE_INTERNAL_ERROR,
) {
    Log.e(tag, message, e)
    ErrorNotification.postErrorNotification(e, message, type)
}

fun warning(
    tag: String, message: String,
    @Type type: Int = CrashReport.CRASH_TYPE_INTERNAL_ERROR,
) {
    Log.w(tag, message)
    ErrorNotification.postErrorNotification(message, type)
}

fun recordThrowable(context: Context?, tag: String, e: Throwable) {
    val report = e.report()
    Log.w(tag, report)
    if (context != null) {
        val externalCacheDir = context.externalCacheDir
        if (externalCacheDir != null && externalCacheDir.isDirectory) {
            val errorStacksFile = File(externalCacheDir, ERROR_STACKS_FILE).also { it.assure() }
            errorStacksFile.appendText(
                "----------------\nError at timestamp(${System.currentTimeMillis()}):\n$report"
            )
        }
    }
}


private fun Throwable.report(): String {
    val writer = StringWriter()

    writer.write(stackTraceText(""))
    writer.write("\n")

    var indent = 2
    var causeThrowable: Throwable? = cause
    while (causeThrowable != null) {
        val padding = " ".repeat(indent + 1)
        writer.write("${padding}# Caused by: \n")
        writer.write(causeThrowable.stackTraceText(padding))
        writer.write("\n")
        causeThrowable = causeThrowable.cause
        indent += 2
    }
    return writer.toString()
}

private fun Throwable.description(): String = "${javaClass.name}: $message"

private fun Throwable.stackTraceText(padding: String): String {
    val head = "$padding# ${description()}"
    return stackTrace.fold(head) { acc, stackTraceElement ->
        "$acc\n$padding- $stackTraceElement"
    }
}

private fun File.assure() {
    if (exists()) {
        if (!isFile) {
            // delete if not a file
            delete()
            createNewFile()
        }
    } else {
        // new files
        createNewFile()
    }
}

fun createDefaultExceptionHandler(
    @Suppress("UNUSED_PARAMETER") TAG: String,
    defaultMessageHeader: String = "Error!",
): CoroutineExceptionHandler =
    CoroutineExceptionHandler { _, exception ->
        ErrorNotification.postErrorNotification(
            exception,
            "$defaultMessageHeader:${exception.message}"
        )
    }

fun startCrashActivity(context: Context, throwable: Throwable, crashActivity: Class<out Activity>) {

    val report =
        if (throwable is InternalDataCorruptedException)
            CrashReport(
                type = CrashReport.CRASH_TYPE_CORRUPTED_DATA,
                note = "Internal data is corrupted, try to wipe application data! ${throwable.cause?.description()}",
                stackTrace = Log.getStackTraceString(throwable.cause),
            )
        else
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