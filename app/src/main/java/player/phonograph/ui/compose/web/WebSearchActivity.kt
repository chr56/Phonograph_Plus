/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import lib.phonograph.activity.ThemeActivity
import lib.phonograph.misc.emit
import player.phonograph.R
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.parcelableExtra
import player.phonograph.util.reportError
import retrofit2.Call
import util.phonograph.tagsources.lastfm.AlbumResult
import util.phonograph.tagsources.lastfm.ArtistResult
import util.phonograph.tagsources.lastfm.LastFMRestClient
import util.phonograph.tagsources.lastfm.LastFMService
import util.phonograph.tagsources.lastfm.LastFmSearchResults
import util.phonograph.tagsources.lastfm.TrackResult
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
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import player.phonograph.model.Album as PhonographAlbum
import player.phonograph.model.Artist as PhonographArtist
import player.phonograph.model.Song as PhonographSong

class WebSearchActivity : ThemeActivity() {

    private val viewModel: WebSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.useCustomStatusBar = false
        super.onCreate(savedInstanceState)

        val query = parseIntent(intent)
        if (query != null) {
            viewModel.prefillQuery(query)
        }

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
                            WebSearchViewModel.Page.Detail -> Detail(viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (viewModel.page.value != WebSearchViewModel.Page.Search) {
            viewModel.updatePage(WebSearchViewModel.Page.Search)
        } else {
            super.onBackPressed()
        }
    }

    private fun parseIntent(intent: Intent): Query? {
        return when (intent.getStringExtra(EXTRA_TYPE)) {
            EXTRA_ALBUM  -> intent.parcelableExtra<PhonographAlbum>(EXTRA_DATA)?.let { Query.from(it) }
            EXTRA_ARTIST -> intent.parcelableExtra<PhonographArtist>(EXTRA_DATA)?.let { Query.from(it) }
            EXTRA_SONG   -> intent.parcelableExtra<PhonographSong>(EXTRA_DATA)?.let { Query.from(it) }
            else         -> null
        }
    }

    companion object {
        const val EXTRA_TYPE = "TYPE"
        const val EXTRA_SONG = "Song"
        const val EXTRA_ARTIST = "Artist"
        const val EXTRA_ALBUM = "Album"
        const val EXTRA_DATA = "DATA"

        fun launchIntent(context: Context, data: PhonographAlbum?): Intent =
            Intent(context, WebSearchActivity::class.java).apply {
                putExtra(EXTRA_TYPE, EXTRA_ALBUM)
                putExtra(EXTRA_DATA, data)
            }

        fun launchIntent(context: Context, data: PhonographArtist?): Intent =
            Intent(context, WebSearchActivity::class.java).apply {
                putExtra(EXTRA_TYPE, EXTRA_ARTIST)
                putExtra(EXTRA_DATA, data)
            }

        fun launchIntent(context: Context, data: PhonographSong?): Intent =
            Intent(context, WebSearchActivity::class.java).apply {
                putExtra(EXTRA_TYPE, EXTRA_SONG)
                putExtra(EXTRA_DATA, data)
            }

        fun launchIntent(context: Context): Intent = Intent(context, WebSearchActivity::class.java)
    }
}

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

    private val _query: MutableStateFlow<Query> = MutableStateFlow(Query())
    val query get() = _query.asStateFlow()

    fun prefillQuery(query: Query) {
        _query.tryEmit(query)
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

    fun search(context: Context, query: Query.QueryAction) {
        lastFmQuery(context) { service ->
            val call = when (query) {
                is Query.QueryAction.Artist  -> service.searchArtist(query.name, 1)
                is Query.QueryAction.Release -> service.searchAlbum(query.name, 1)
                is Query.QueryAction.Track   -> service.searchTrack(query.name, query.artist, 1)
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
        viewModelScope.launch(Dispatchers.IO) {
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