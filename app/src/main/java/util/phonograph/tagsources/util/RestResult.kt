/*
 *  Copyright (c) 2022~2024 chr_56
 */

package util.phonograph.tagsources.util

import androidx.annotation.Keep

@Keep
sealed class RestResult<T>(val isSuccess: Boolean) {

    fun dataOrNull() = (this as? Success)?.data
    fun messageOrNull() = (this as? RemoteError)?.message
    fun exceptionOrNull() = (this as? ParseError)?.exception

    class Success<T>(val data: T) : RestResult<T>(true)
    class RemoteError<T>(val message: String) : RestResult<T>(false)
    class ParseError<T>(val exception: Throwable) : RestResult<T>(false)
    class NetworkError<T>(val exception: Throwable) : RestResult<T>(false)
}