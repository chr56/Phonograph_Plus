/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.web.LastFmAction.View
import util.phonograph.tagsources.lastfm.AlbumResult
import util.phonograph.tagsources.lastfm.ArtistResult
import util.phonograph.tagsources.lastfm.LastFmSearchResultItem
import util.phonograph.tagsources.lastfm.TrackResult
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
fun LastFmSearch(viewModel: WebSearchViewModel, pageState: Page.Search.LastFmSearch) {
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
        val onSelect: (LastFmSearchResultItem) -> Unit = { selected ->
            viewModel.viewModelScope.launch {
                val action = when (selected) {
                    is AlbumResult.Album   -> View.ViewAlbum(selected)
                    is ArtistResult.Artist -> View.ViewArtist(selected)
                    is TrackResult.Track   -> View.ViewTrack(selected)
                }
                val result = queryState.query(context, action).await() ?: return@launch //todo
                val page = Page.Detail.LastFmDetail(result)
                viewModel.navigator.navigateTo(page)
            }
        }
        LastFmSearchResult(searchResults, onSelect, Modifier.align(Alignment.CenterHorizontally))

    }
}

@Composable
fun MusicBrainzSearch(viewModel: WebSearchViewModel, pageState: Page.Search.MusicBrainzSearch) {
    val queryState by pageState.query.collectAsState()
    Column {
        val context = LocalContext.current
        MusicBrainzSearchBox(
            Modifier.wrapContentHeight(), queryState
        ) {
            queryState.query(context, it)
        }

        val searchResults by queryState.result.collectAsState()
        val onSelect: (MusicBrainzAction.View) -> Unit = { action ->
            viewModel.viewModelScope.launch {
                val result = queryState.query(context, action).await() ?: return@launch //todo
                val page = Page.Detail.MusicBrainzDetail(result)
                viewModel.navigator.navigateTo(page)
            }
        }
        MusicBrainzSearchResult(searchResults, Modifier.align(Alignment.CenterHorizontally), onSelect)
    }
}
