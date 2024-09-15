/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.misc

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException

@Throws(IOException::class)
suspend fun <T> Call<T?>.emit(): Result<Response<T?>> =
    suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback<T?> {
            override fun onResponse(call: Call<T?>, response: Response<T?>) {
                continuation.resume(Result.success(response)) { _, _, _ -> }
            }

            override fun onFailure(call: Call<T?>, t: Throwable) {
                if (continuation.isCancelled) {
                    continuation.cancel()
                } else {
                    continuation.resume(Result.failure(t)) { _, _, _ -> }
                }
            }
        })
    }

@JvmName("emit_rest_result")
@Throws(IOException::class)
suspend fun <T> Call<RestResult<T>?>.emit(): RestResult<T> =
    suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback<RestResult<T>?> {

            override fun onResponse(call: Call<RestResult<T>?>, response: Response<RestResult<T>?>) {
                continuation.resume(
                    response.body() ?: RestResult.RemoteError(response.errorBody()?.string().orEmpty())
                ) { _, _, _ -> }
            }

            override fun onFailure(call: Call<RestResult<T>?>, t: Throwable) {
                if (continuation.isCancelled) {
                    continuation.cancel()
                } else {
                    continuation.resume(RestResult.NetworkError(t)) { _, _, _ -> }
                }
            }
        })
    }
