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


@Composable
fun Search(viewModel: WebSearchViewModel) {
    val queryState by viewModel.query.collectAsState()

    val query = queryState
    if (query is LastFmQuery) {
        LastFmSearch(viewModel, query)
    }
}

@Composable
fun LastFmSearch(viewModel: WebSearchViewModel, queryState: LastFmQuery) {
    Column {
        val context = LocalContext.current
        LastFmSearchBox(
            lastFmQuery = queryState,
            Modifier.wrapContentHeight()
        ) {
            queryState.query(context, it)
        }

        val result by queryState.result.collectAsState()
        val onSelect: (LastFmSearchResultItem) -> Unit = {
            val action = queryState.viewAction(it)
            queryState.query(context, action)
            viewModel.updatePage(WebSearchViewModel.Page.Detail)
        }
        LastFmSearchResult(result, onSelect, Modifier.align(Alignment.CenterHorizontally))

    }
}