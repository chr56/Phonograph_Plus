/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import lib.phonograph.misc.RestResult
import lib.phonograph.misc.emit
import player.phonograph.util.reportError
import player.phonograph.util.warning
import retrofit2.Call
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.StateFlow

sealed class Query<P : QueryParameter, A : WebSearchAction>(viewModel: ViewModel, val source: String) {

    abstract val queryParameter: StateFlow<P>
    abstract fun updateQueryParameter(update: (P) -> P)
    abstract fun query(context: Context, action: A): Deferred<*>

    protected suspend fun <T> Call<RestResult<T>?>.tryExecute(): T? {
        // todo
        when (val result = emit<T>()) {
            is RestResult.ParseError   -> reportError(result.exception, TAG, "Parse error!")
            is RestResult.NetworkError -> reportError(result.exception, TAG, "Network error!")
            is RestResult.RemoteError  -> warning(TAG, result.message)
            is RestResult.Success      -> return result.data
        }
        return null
    }

    companion object {
        private const val TAG = "WebSearch"
    }

    protected val viewModelScope = viewModel.viewModelScope
}