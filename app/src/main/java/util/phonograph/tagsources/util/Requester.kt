/*
 *  Copyright (c) 2022~2024 chr_56
 */

package util.phonograph.tagsources.util

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException

@JvmName("emit_rest_result")
@OptIn(ExperimentalCoroutinesApi::class)
@Throws(IOException::class)
suspend fun <T> Call<RestResult<T>?>.emit(): RestResult<T> =
    suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback<RestResult<T>?> {

            override fun onResponse(call: Call<RestResult<T>?>, response: Response<RestResult<T>?>) {
                continuation.resume(
                    response.body() ?: RestResult.RemoteError(response.errorBody()?.string().orEmpty())
                ) {}
            }

            override fun onFailure(call: Call<RestResult<T>?>, t: Throwable) {
                if (continuation.isCancelled) {
                    continuation.cancel()
                } else {
                    continuation.resume(RestResult.NetworkError(t)) { }
                }
            }
        })
    }
