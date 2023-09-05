/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import util.phonograph.tagsources.lastfm.LastFmSearchResultItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


@Composable
fun LastFmSearch(viewModel: WebSearchViewModel, pageState: WebSearchViewModel.Page.Search.LastFmSearch) {
    val queryState by pageState.query.collectAsState()
    Column {
        val context = LocalContext.current
        LastFmSearchBox(
            lastFmQuery = queryState,
            Modifier.wrapContentHeight()
        ) {
            queryState.query(context, it)
        }

        val searchResults by queryState.result.collectAsState()
        val onSelect: (LastFmSearchResultItem) -> Unit = {
            viewModel.viewModelScope.launch {
                val action = queryState.viewAction(it)
                val result = queryState.query(context, action).await() ?: return@launch //todo
                val page = WebSearchViewModel.Page.Detail.LastFmDetail(result)
                viewModel.navigator.navigateTo(page)
            }
        }
        LastFmSearchResult(searchResults, onSelect, Modifier.align(Alignment.CenterHorizontally))

    }
}

@Composable
fun MusicBrainzSearch(viewModel: WebSearchViewModel, pageState: WebSearchViewModel.Page.Search.MusicBrainzSearch) {
    Column {
        val context = LocalContext.current

        // todo MusicBrainzSearchBox

        // todo MusicBrainzSearchResult
    }
}
