/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util

import player.phonograph.notification.ErrorNotification
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

private const val ERROR_STACKS_FILE = "errors.txt"

fun reportError(e: Throwable, tag: String, message: String) {
    Log.e(tag, message, e)
    ErrorNotification.postErrorNotification(e, message)
}

fun warning(tag: String, message: String) {
    Log.w(tag, message)
    ErrorNotification.postErrorNotification(message)
}

fun recordThrowable(context: Context?, tag: String, e: Throwable) {
    val report = e.report()
    Log.w(tag, report)
    if (context != null) {
        val externalCacheDir = context.externalCacheDir
        if (externalCacheDir != null && externalCacheDir.isDirectory) {
            val errorStacksFile = File(externalCacheDir, ERROR_STACKS_FILE).also { it.assure() }
            errorStacksFile.appendText(
                "----------------\nError at time ${System.currentTimeMillis()}:\n$report"
            )
        }
    }
}


private fun Throwable.report(): String {
    val writer = StringWriter(256)
    PrintWriter(writer).use { printWriter ->
        printStackTrace(printWriter)
        val causeThrowable = cause
        if (causeThrowable != null) {
            printWriter.print(causeThrowable.report())
        }
    }
    val stackTrace = writer.toString()
    return "Error: $message\n$stackTrace"
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