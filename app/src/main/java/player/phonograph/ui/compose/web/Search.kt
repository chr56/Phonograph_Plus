/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
fun LastFmSearch(viewModel: WebSearchViewModel) {
    Column {
        val queryState by viewModel.query.collectAsState()
        val context = LocalContext.current
        LastFmSearchBox(
            query = queryState,
            Modifier
                .wrapContentHeight()
                // .background(MaterialTheme.colors.surface)
        ) {
            viewModel.search(context, it)
        }

        val result by viewModel.result.collectAsState()
        val onSelect: (Any) -> Unit = {
            viewModel.select(context, it)
            viewModel.updatePage(WebSearchViewModel.Page.Detail)
        }
        LastFmSearchResult(result, onSelect, Modifier.align(Alignment.CenterHorizontally))

    }
}