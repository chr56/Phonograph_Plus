/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import util.phonograph.tagsources.lastfm.LastFmAlbumResponse
import util.phonograph.tagsources.lastfm.LastFmArtistResponse
import util.phonograph.tagsources.lastfm.LastFmSearchResultResponse
import util.phonograph.tagsources.lastfm.LastFmSearchResults
import util.phonograph.tagsources.lastfm.LastFmTrackResponse
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun LastFmSearch(viewModel: WebSearchViewModel, page: PageSearch.LastFmSearch) {
    val parameterState by page.queryParameter.collectAsState()
    var searchResults: LastFmSearchResults? by remember { mutableStateOf(null) }
    Column {
        val context = LocalContext.current
        LastFmSearchBox(
            parameterState,
            page::updateQueryParameter,
            Modifier.wrapContentHeight()
        ) { action ->
            val delegate = viewModel.clientDelegateLastFm(context)
            val deferred = delegate.request(context, action)
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                when (val respond = deferred.await()) {
                    is LastFmSearchResultResponse -> searchResults = respond.results
                    else                          -> {}
                }
            }
        }

        LastFmSearchResult(searchResults, Modifier.align(Alignment.CenterHorizontally)) { action ->
            viewModel.viewModelScope.launch {
                val delegate = viewModel.clientDelegateLastFm(context)
                val detailPage =
                    when (val response = delegate.request(context, action).await()) {
                        is LastFmAlbumResponse  -> PageDetail.LastFmDetail(response.album)
                        is LastFmArtistResponse -> PageDetail.LastFmDetail(response.artist)
                        is LastFmTrackResponse  -> PageDetail.LastFmDetail(response.track)
                        else                    -> null
                    }
                if (detailPage != null) {
                    viewModel.navigator.navigateTo(detailPage)
                }
            }
        }

    }
}

