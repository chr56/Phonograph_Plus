/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import lib.phonograph.activity.ThemeActivity
import lib.phonograph.misc.emit
import player.phonograph.R
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.reportError
import player.phonograph.util.warning
import util.phonograph.tagsources.lastfm.LastFMRestClient
import util.phonograph.tagsources.lastfm.LastFmSearchResults
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebSearchActivity : ThemeActivity() {

    private val viewModel: WebSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.useCustomStatusBar = false
        super.onCreate(savedInstanceState)
        setContent {
            PhonographTheme {

                val pageState by viewModel.page.collectAsState()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(id = R.string.web_search)) }
                        )
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxWidth()
                    ) {

                        when (pageState) {
                            WebSearchViewModel.Page.Search -> LastFmSearch(viewModel)
                            WebSearchViewModel.Page.Detail -> {}
                        }
                    }
                }
            }
        }
    }
}

class WebSearchViewModel : ViewModel() {

    private val _page: MutableStateFlow<Page> = MutableStateFlow(Page.Search)
    val page get() = _page.asStateFlow()

    sealed class Page {
        object Search : Page()
        object Detail : Page()
    }

    private val _query: MutableStateFlow<Query> = MutableStateFlow(Query())
    val query get() = _query.asStateFlow()


    private val _result: MutableStateFlow<LastFmSearchResults?> = MutableStateFlow(null)
    val result get() = _result.asStateFlow()


    private var lastFMRestClient: LastFMRestClient? = null

    fun search(context: Context, query: Query.QueryAction) {
        if (lastFMRestClient == null) lastFMRestClient = LastFMRestClient(context)
        viewModelScope.launch(Dispatchers.IO) {
            val service = lastFMRestClient?.apiService
            if (service != null) {
                val call = when (query) {
                    is Query.QueryAction.Artist -> service.searchArtist(query.name, 1)
                    is Query.QueryAction.Release -> service.searchAlbum(query.name, 1)
                    is Query.QueryAction.Track -> service.searchTrack(query.name, query.artist, 1)
                }
                val response = call.emit()
                if (response.isSuccess) {
                    val searchResult = response.getOrNull()?.body()
                    if (searchResult != null) {
                        _result.emit(searchResult.results)
                    } else {
                        warning(TAG, ERR_MSG)
                    }
                } else {
                    reportError(response.exceptionOrNull() ?: Exception(), TAG, ERR_MSG)
                }
            }
        }
    }

    companion object {
        private const val TAG = "WebSearch"
        private const val ERR_MSG = "Failed to query!\n"
    }

}