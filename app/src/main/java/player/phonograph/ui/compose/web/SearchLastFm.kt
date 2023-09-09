/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.web.LastFmAction.View
import util.phonograph.tagsources.lastfm.AlbumResult
import util.phonograph.tagsources.lastfm.ArtistResult
import util.phonograph.tagsources.lastfm.LastFmAlbumResponse
import util.phonograph.tagsources.lastfm.LastFmArtistResponse
import util.phonograph.tagsources.lastfm.LastFmSearchResultItem
import util.phonograph.tagsources.lastfm.LastFmSearchResultResponse
import util.phonograph.tagsources.lastfm.LastFmSearchResults
import util.phonograph.tagsources.lastfm.LastFmTrackResponse
import util.phonograph.tagsources.lastfm.TrackResult
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
        ) {
            val delegate = viewModel.clientDelegateLastFm(context)
            val deferred = delegate.request(context, it)
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                when (val respond = deferred.await()) {
                    is LastFmSearchResultResponse -> searchResults = respond.results
                    else                          -> {}
                }
            }
        }

        val onSelect: (LastFmSearchResultItem) -> Unit = { selected ->
            viewModel.viewModelScope.launch {
                val action = when (selected) {
                    is AlbumResult.Album   -> View.ViewAlbum(selected)
                    is ArtistResult.Artist -> View.ViewArtist(selected)
                    is TrackResult.Track   -> View.ViewTrack(selected)
                }
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
        LastFmSearchResult(searchResults, onSelect, Modifier.align(Alignment.CenterHorizontally))

    }
}

