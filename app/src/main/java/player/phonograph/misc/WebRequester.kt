/*
 * Copyright (c) 2022 chr_56
 */

@file:JvmName("WebRequester")

package player.phonograph.misc

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lib.phonograph.misc.emit
import okhttp3.OkHttpClient
import okhttp3.Request
 import okhttp3.Response
import java.io.IOException
import java.io.PrintStream
import java.io.PrintWriter
import java.util.concurrent.TimeUnit

private const val TAG = "WebRequester"

internal val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .build()

suspend fun webRequest(request: Request): Response {
    return withContext(Dispatchers.IO) {
        val call = okHttpClient.newCall(request)
        return@withContext try {
            call.emit()
        } catch (e: IOException) {
            throw NetworkException(request, e)
        }
    }
}

/**
 * An IOException Wrapper with [okhttp3.Request]
 */
class NetworkException : IOException {

    var delegate: IOException
        private set

    var request: Request? = null
        private set

    constructor(e: IOException) : super(e) {
        delegate = e
    }

    constructor(request: Request, e: IOException) : this(e) {
        this.request = request
    }

    // --------------------
    // delegate by IOException
    // --------------------

    override fun getLocalizedMessage(): String? = message

    override fun initCause(cause: Throwable?): Throwable = delegate.initCause(cause)

    override fun printStackTrace() {
        delegate.printStackTrace()
    }

    override fun printStackTrace(s: PrintStream) {
        delegate.printStackTrace(s)
    }

    override fun printStackTrace(s: PrintWriter) {
        delegate.printStackTrace(s)
    }

    override fun fillInStackTrace(): Throwable = delegate.fillInStackTrace()

    override fun getStackTrace(): Array<StackTraceElement> = delegate.stackTrace

    override fun setStackTrace(stackTrace: Array<out StackTraceElement>) {
        delegate.stackTrace = stackTrace
    }

    override val cause: Throwable? get() = delegate.cause
    override val message: String? get() = delegate.message
}
