/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources

import lib.phonograph.misc.RestResult
import lib.phonograph.misc.emit
import player.phonograph.util.reportError
import player.phonograph.util.warning
import retrofit2.Call
import android.content.Context
import kotlinx.coroutines.Deferred

abstract class AbsClientDelegate<A : Action, R> {

    abstract fun request(context: Context, action: A): Deferred<R?>

    /**
     * unwrap [RestResult]
     */
    protected suspend fun <T> Call<RestResult<T>?>.process(): T? {
        when (val result = emit<T>()) {
            is RestResult.ParseError   -> reportError(result.exception, TAG, "Parse error!")
            is RestResult.NetworkError -> reportError(result.exception, TAG, "Network error!")
            is RestResult.RemoteError  -> warning(TAG, result.message)
            is RestResult.Success      -> return result.data
        }
        return null
    }

    protected companion object {
        private const val TAG = "WebSearch"
    }
}