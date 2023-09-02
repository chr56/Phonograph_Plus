/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.misc

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@Throws(IOException::class)
suspend fun <T> Call<T?>.emit(): Result<Response<T?>> =
    suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback<T?> {
            override fun onResponse(call: Call<T?>, response: Response<T?>) {
                continuation.resume(Result.success(response)) { }
            }

            override fun onFailure(call: Call<T?>, t: Throwable) {
                if (continuation.isCancelled) {
                    continuation.cancel()
                } else {
                    continuation.resume(Result.failure(t)) { }
                }
            }
        })
    }
