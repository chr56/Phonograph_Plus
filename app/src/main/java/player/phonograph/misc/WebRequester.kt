/*
 * Copyright (c) 2022 chr_56
 */

@file:JvmName("WebRequester")

package player.phonograph.misc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lib.phonograph.misc.emit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
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
            throw e
        }
    }
}
