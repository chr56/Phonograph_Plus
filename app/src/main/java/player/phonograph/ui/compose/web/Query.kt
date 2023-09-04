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

sealed class Query<A : Query.Action>(viewModel: ViewModel, val source: String) {

    abstract fun search(context: Context, action: A)

    abstract fun view(context: Context, item: Any)

    protected suspend fun <T> execute(call: Call<T?>): T? {
        val result = call.emit<T>()
        return if (result.isSuccess) {
            result.getOrNull()?.body()
        } else {
            reportError(result.exceptionOrNull() ?: Exception(), TAG, ERR_MSG)
            null
        }
    }

    interface Action

    companion object {
        private const val TAG = "WebSearch"
        private const val ERR_MSG = "Failed to query!\n"
    }

    protected val viewModelScope = viewModel.viewModelScope
}