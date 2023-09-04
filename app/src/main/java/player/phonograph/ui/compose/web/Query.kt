/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import lib.phonograph.misc.emit
import player.phonograph.util.reportError
import retrofit2.Call
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

sealed class Query<P : Query.Parameter, A : Query.Action>(viewModel: ViewModel, val source: String) {

    abstract val queryParameter: StateFlow<P>
    abstract fun updateQueryParameter(update: (P) -> P)
    abstract fun query(context: Context, action: A)

    protected suspend fun <T> Call<T?>.tryExecute(): T? {
        val result = emit<T>()
        return if (result.isSuccess) {
            result.getOrNull()?.body()
        } else {
            reportError(result.exceptionOrNull() ?: Exception(), TAG, ERR_MSG)
            null
        }
    }

    interface Action
    interface Parameter

    companion object {
        private const val TAG = "WebSearch"
        private const val ERR_MSG = "Failed to query!\n"
    }

    protected val viewModelScope = viewModel.viewModelScope
}