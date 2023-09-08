/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import lib.phonograph.misc.emit
import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.ui.compose.base.BridgeDialogFragment
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.ui.compose.web.LastFmAlbum
import player.phonograph.ui.compose.web.LastFmArtist
import player.phonograph.ui.compose.web.LastFmTrack
import player.phonograph.ui.compose.web.WebSearchActivity
import player.phonograph.util.parcelable
import player.phonograph.util.warning
import retrofit2.Call
import retrofit2.Response
import util.phonograph.tagsources.lastfm.LastFMRestClient
import util.phonograph.tagsources.lastfm.LastFMService
import util.phonograph.tagsources.lastfm.LastFmModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import util.phonograph.tagsources.lastfm.LastFmAlbum as LastFmAlbumModel
import util.phonograph.tagsources.lastfm.LastFmArtist as LastFmArtistModel
import util.phonograph.tagsources.lastfm.LastFmTrack as LastFmTrackModel

class LastFmDialog : BridgeDialogFragment() {

    private lateinit var lastFMRestClient: LastFMRestClient

    private val viewModel: LastFmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastFMRestClient = LastFMRestClient(requireContext())

        viewModel.mode = requireArguments().getString(EXTRA_TYPE)

        when (viewModel.mode) {
            TYPE_ARTIST -> {
                val artist = requireArguments().parcelable<Artist>(EXTRA_DATA)
                if (artist != null) {
                    viewModel.loadArtist(requireContext(), lastFMRestClient.apiService, artist)
                }
            }

            TYPE_ALBUM  -> {
                val album = requireArguments().parcelable<Album>(EXTRA_DATA)
                if (album != null) {
                    viewModel.loadAlbum(requireContext(), lastFMRestClient.apiService, album)
                }
            }

            TYPE_SONG   -> {
                val song = requireArguments().parcelable<Song>(EXTRA_DATA)
                if (song != null) {
                    viewModel.loadSong(requireContext(), lastFMRestClient.apiService, song)
                }
            }
        }
    }

    @Composable
    override fun Content() {
        val dialogState = rememberMaterialDialogState(true)
        PhonographTheme {
            MaterialDialog(
                dialogState = dialogState,
                elevation = 0.dp,
                onCloseRequest = { dismiss() },
                buttons = {
                    negativeButton(
                        res = R.string.web_search,
                        textStyle = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
                    ) {
                        val context = requireContext()
                        val arguments = requireArguments()

                        val launchIntent: Intent = when (viewModel.mode) {
                            TYPE_ARTIST -> WebSearchActivity.searchLastFmArtist(
                                context, arguments.parcelable(EXTRA_DATA)
                            )

                            TYPE_ALBUM  -> WebSearchActivity.searchLastFmAlbum(
                                context, arguments.parcelable(EXTRA_DATA)
                            )

                            TYPE_SONG   -> WebSearchActivity.searchLastFmSong(
                                context, arguments.parcelable(EXTRA_DATA)
                            )

                            else        -> WebSearchActivity.launchIntent(context)
                        }

                        dismiss()
                        context.startActivity(launchIntent)

                    }
                    button(
                        res = android.R.string.ok,
                        textStyle = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
                    ) {
                        dismiss()
                    }
                }
            ) {
                Box(Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.wiki),
                        style = MaterialTheme.typography.h5
                    )
                }
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize(0.97f)
                        .padding(12.dp),
                    Alignment.Center
                ) {
                    val result by viewModel.response.collectAsState()
                    when (val item = result) {
                        is LastFmAlbumModel -> LastFmAlbum(item)
                        is LastFmArtistModel -> LastFmArtist(item)
                        is LastFmTrackModel -> LastFmTrack(item)
                        null -> Text(stringResource(R.string.wiki_unavailable))
                    }
                }
            }
        }

    }


    class LastFmViewModel : ViewModel() {

        var mode: String? = null

        private val _response: MutableStateFlow<LastFmModel?> = MutableStateFlow(null)
        val response get() = _response.asStateFlow()

        fun loadArtist(context: Context, lastFMService: LastFMService, artist: Artist) {
            viewModelScope.launch(Dispatchers.IO) {
                val response = execute(
                    listOf(
                        lastFMService.getArtistInfo(
                            artistName = artist.name,
                            language = Locale.getDefault().language,
                            cacheControl = null
                        ),
                        lastFMService.getArtistInfo(
                            artistName = artist.name,
                            language = null,
                            cacheControl = null
                        )
                    )
                )
                _response.update { response?.body()?.artist }
            }
        }

        fun loadAlbum(context: Context, lastFMService: LastFMService, album: Album) {
            viewModelScope.launch(Dispatchers.IO) {
                val response = execute(
                    listOf(
                        lastFMService.getAlbumInfo(
                            albumName = album.title,
                            artistName = album.artistName,
                            language = Locale.getDefault().language,
                        ),
                        lastFMService.getAlbumInfo(
                            albumName = album.title,
                            artistName = album.artistName,
                            language = null,
                        )
                    )
                )
                _response.update { response?.body()?.album }
            }
        }


        fun loadSong(context: Context, lastFMService: LastFMService, song: Song) {
            viewModelScope.launch(Dispatchers.IO) {
                val response = execute(
                    listOf(
                        lastFMService.getTrackInfo(
                            name = song.title,
                            artistName = song.artistName,
                            language = Locale.getDefault().language,
                        ),
                        lastFMService.getTrackInfo(
                            name = song.title,
                            artistName = song.artistName,
                            language = null,
                        )
                    )
                )
                _response.update { response?.body()?.track }
            }
        }

        private suspend fun <T> execute(
            calls: List<Call<T?>>,
        ): Response<T?>? {
            var latestError: Throwable? = null
            for (call in calls) {
                val result = call.emit<T>()
                if (result.isSuccess) {
                    return result.getOrNull()
                } else {
                    latestError = result.exceptionOrNull()
                }
            }
            if (latestError != null)
                warning(
                    TAG,
                    "${latestError.javaClass.simpleName}: ${latestError.message}\n${latestError.stackTraceToString()}"
                )
            return null
        }

    }

    companion object {

        private const val TAG = "LastFM"

        private const val EXTRA_TYPE = "type"

        private const val TYPE_ARTIST = "artist"
        private const val TYPE_ALBUM = "album"
        private const val TYPE_SONG = "song"

        private const val EXTRA_DATA = "data"

        fun from(artist: Artist) = LastFmDialog().apply {
            arguments = Bundle().apply {
                putString(EXTRA_TYPE, TYPE_ARTIST)
                putParcelable(EXTRA_DATA, artist)
            }
        }

        fun from(album: Album) = LastFmDialog().apply {
            arguments = Bundle().apply {
                putString(EXTRA_TYPE, TYPE_ALBUM)
                putParcelable(EXTRA_DATA, album)
            }
        }

        fun from(song: Song) = LastFmDialog().apply {
            arguments = Bundle().apply {
                putString(EXTRA_TYPE, TYPE_SONG)
                putParcelable(EXTRA_DATA, song)
            }
        }
    }
}