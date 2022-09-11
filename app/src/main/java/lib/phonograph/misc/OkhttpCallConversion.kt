/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.misc

import java.io.IOException
import kotlin.coroutines.resumeWithException
import kotlin.jvm.Throws
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

@OptIn(ExperimentalCoroutinesApi::class)
@Throws(IOException::class)
suspend fun Call.emit(reportFailure: Boolean = true): Response =
    suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { }
            }

            override fun onFailure(call: Call, e: IOException) {
                if (!reportFailure || continuation.isCancelled) {
                    continuation.cancel()
                } else {
                    continuation.resumeWithException(e)
                }
            }
        })
    }
