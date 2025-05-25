/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.error

import java.io.StringWriter

object ExceptionDescriptions {

    fun description(throwable: Throwable): String = "${throwable.javaClass.name}: ${throwable.message}"

    fun stacktrace(throwable: Throwable, padding: String): String {
        val head = "$padding# ${description(throwable)}"
        return throwable.stackTrace.fold(head) { acc, stackTraceElement ->
            "$acc\n$padding- $stackTraceElement"
        }
    }

    fun report(throwable: Throwable): String {
        val writer = StringWriter()

        writer.write(stacktrace(throwable, ""))
        writer.write("\n")

        var indent = 2
        var causeThrowable: Throwable? = throwable.cause
        while (causeThrowable != null) {
            val padding = " ".repeat(indent + 1)
            writer.write("${padding}# Caused by: \n")
            writer.write(stacktrace(throwable, padding))
            writer.write("\n")
            causeThrowable = causeThrowable.cause
            indent += 2
        }
        return writer.toString()
    }
}
