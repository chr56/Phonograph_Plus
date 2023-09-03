/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import lib.phonograph.misc.emit
import player.phonograph.util.reportError
import retrofit2.Call
import util.phonograph.tagsources.lastfm.AlbumResult
import util.phonograph.tagsources.lastfm.ArtistResult
import util.phonograph.tagsources.lastfm.LastFMRestClient
import util.phonograph.tagsources.lastfm.LastFMService
import util.phonograph.tagsources.lastfm.LastFmSearchResults
import util.phonograph.tagsources.lastfm.TrackResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebSearchViewModel : ViewModel() {

    private val _page: MutableStateFlow<Page> = MutableStateFlow(Page.Search)
    val page get() = _page.asStateFlow()

    fun updatePage(page: Page) {
        _page.value = page
    }

    sealed class Page {
        object Search : Page()
        object Detail : Page()
    }

    private val _query: MutableStateFlow<Query> = MutableStateFlow(LastFmQuery())
    val query get() = _query.asStateFlow()

    fun prefillQuery(lastFmQuery: LastFmQuery) {
        _query.tryEmit(lastFmQuery)
    }


    private val _result: MutableStateFlow<LastFmSearchResults?> = MutableStateFlow(null)
    val result get() = _result.asStateFlow()


    private val _selectedItem: MutableStateFlow<Any?> = MutableStateFlow(null)
    val selectedItem get() = _selectedItem.asStateFlow()

    private val _detail: MutableStateFlow<Any?> = MutableStateFlow(null)
    val detail get() = _detail.asStateFlow()

    fun select(context: Context, item: Any) {
        _selectedItem.value = item
        viewModelScope.launch(Dispatchers.IO) {
            when (item) {
                is AlbumResult.Album   -> queryLastFMAlbum(context, item)
                is ArtistResult.Artist -> queryLastFMArtist(context, item)
                is TrackResult.Track   -> queryLastFMTrack(context, item)
            }
        }
    }


    private var lastFMRestClient: LastFMRestClient? = null
    private var lastFmQueryJob: Job? = null

    fun search(context: Context, action: LastFmQuery.QueryAction) {
        lastFmQuery(context) { service ->
            val call = when (action) {
                is LastFmQuery.QueryAction.Artist  -> service.searchArtist(action.name, 1)
                is LastFmQuery.QueryAction.Release -> service.searchAlbum(action.name, 1)
                is LastFmQuery.QueryAction.Track   -> service.searchTrack(action.name, action.artist, 1)
            }
            val searchResult = execute(call)
            if (searchResult != null) {
                _result.emit(searchResult.results)
            }
        }

    }

    private fun queryLastFMAlbum(context: Context, album: AlbumResult.Album) {
        lastFmQuery(context) { service ->
            val call = service.getAlbumInfo(album.name, album.artist, null)
            val response = execute(call)
            _detail.emit(response?.album)
        }
    }

    private fun queryLastFMArtist(context: Context, artist: ArtistResult.Artist) {
        lastFmQuery(context) { service ->
            val call = service.getArtistInfo(artist.name, null, null)
            val response = execute(call)
            _detail.emit(response?.artist)
        }
    }

    private fun queryLastFMTrack(context: Context, track: TrackResult.Track) {
        lastFmQuery(context) { service ->
            val call = service.getTrackInfo(track.name, track.artist, null)
            val response = execute(call)
            _detail.emit(response?.track)
        }
    }

    private fun lastFmQuery(context: Context, block: suspend CoroutineScope.(LastFMService) -> Unit) {
        if (lastFMRestClient == null) lastFMRestClient = LastFMRestClient(context)
        lastFmQueryJob?.cancel()
        lastFmQueryJob = viewModelScope.launch(Dispatchers.IO) {
            val service = lastFMRestClient?.apiService
            if (service != null) {
                block.invoke(this, service)
            }
        }
    }

    private suspend fun <T> execute(call: Call<T?>): T? {
        val result = call.emit<T>()
        return if (result.isSuccess) {
            result.getOrNull()?.body()
        } else {
            reportError(result.exceptionOrNull() ?: Exception(), TAG, ERR_MSG)
            null
        }
    }

    companion object {
        private const val TAG = "WebSearch"
        private const val ERR_MSG = "Failed to query!\n"
    }

}