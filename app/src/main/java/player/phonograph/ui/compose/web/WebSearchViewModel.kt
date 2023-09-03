/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import androidx.lifecycle.ViewModel
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebSearchViewModel : ViewModel() {

    sealed class Page {
        object Search : Page()
        object Detail : Page()
    }

    private val _page: MutableStateFlow<Page> = MutableStateFlow(Page.Search)
    val page get() = _page.asStateFlow()

    fun updatePage(page: Page) {
        _page.value = page
    }

    private val _query: MutableStateFlow<Query?> = MutableStateFlow(null)
    val query get() = _query.asStateFlow()

    fun prepareQuery(context: Context, query: Query?) {
        _query.tryEmit(query ?: LastFmQuery(context))
    }

}