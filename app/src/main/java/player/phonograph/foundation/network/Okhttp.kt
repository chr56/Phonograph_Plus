/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.foundation.network

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(4500, TimeUnit.MILLISECONDS)
    .build()


suspend fun invokeHttpRequest(request: Request): Response {
    return withContext(Dispatchers.IO) {
        val call = okHttpClient.newCall(request)
        return@withContext run {
            val result = call.emit()
            result.getOrNull() ?: throw result.exceptionOrNull()!!
        }
    }
}


@Throws(IOException::class)
private suspend fun Call.emit(reportFailure: Boolean = true): Result<Response> =
    suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(Result.success(response)) { _, _, _ -> }
            }

            override fun onFailure(call: Call, e: IOException) {
                if (!reportFailure || continuation.isCancelled) {
                    continuation.cancel()
                } else {
                    continuation.resume(Result.failure(e)) { _, _, _ -> }
                }
            }
        })
    }