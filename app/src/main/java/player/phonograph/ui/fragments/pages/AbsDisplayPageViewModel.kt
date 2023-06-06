/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.ui.fragments.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * @param IT the model type that this fragment displays
 */
abstract class AbsDisplayPageViewModel<IT> : ViewModel() {

    private val _dataSet: MutableStateFlow<Collection<IT>> = MutableStateFlow(emptyList())
    val dataSet: StateFlow<Collection<IT>> get() = _dataSet.asStateFlow()


    val isEmpty get() = dataSet.value.isEmpty()
    // val isEmptyFlow: Flow<Boolean> = _dataSet.map { it.isEmpty() }

    private var job: Job? = null
    fun loadDataset(context: Context) {
        job?.cancel()
        job = viewModelScope.launch {
            _dataSet.emit(
                loadDataSetImpl(context, this)
            )
        }
    }

    abstract suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<IT>


    abstract val headerTextRes: Int
    fun headerText(context: Context): CharSequence? {
        if (headerTextRes <= 0) return null
        val n = dataSet.value.size
        return context.resources.getQuantityString(headerTextRes, n, n)
    }

}