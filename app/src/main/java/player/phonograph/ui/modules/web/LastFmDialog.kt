/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import player.phonograph.R
import player.phonograph.USER_AGENT
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.ui.compose.BridgeDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.util.parcelable
import util.phonograph.tagsources.AbsClientDelegate
import util.phonograph.tagsources.lastfm.AlbumResult
import util.phonograph.tagsources.lastfm.ArtistResult
import util.phonograph.tagsources.lastfm.LastFmAction
import util.phonograph.tagsources.lastfm.LastFmAlbumResponse
import util.phonograph.tagsources.lastfm.LastFmArtistResponse
import util.phonograph.tagsources.lastfm.LastFmClientDelegate
import util.phonograph.tagsources.lastfm.LastFmModel
import util.phonograph.tagsources.lastfm.LastFmTrackResponse
import util.phonograph.tagsources.lastfm.TrackResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

    private val viewModel: LastFmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.prepareDelegate(requireContext())
        viewModel.mode = requireArguments().getString(EXTRA_TYPE)

        when (viewModel.mode) {
            TYPE_ARTIST -> {
                val artist = requireArguments().parcelable<Artist>(EXTRA_DATA)
                if (artist != null) {
                    viewModel.loadArtist(requireContext(), artist)
                    viewModel.target = artist
                }
            }

            TYPE_ALBUM  -> {
                val album = requireArguments().parcelable<Album>(EXTRA_DATA)
                if (album != null) {
                    viewModel.loadAlbum(requireContext(), album)
                    viewModel.target = album
                }
            }

            TYPE_SONG   -> {
                val song = requireArguments().parcelable<Song>(EXTRA_DATA)
                if (song != null) {
                    viewModel.loadSong(requireContext(), song)
                    viewModel.target = song
                }
            }
        }
    }

    @Composable
    override fun Content() {
        val dialogState = rememberMaterialDialogState(true)
        val webSearchDialogState = rememberMaterialDialogState(false)
        PhonographTheme {
            MaterialDialog(
                dialogState = dialogState,
                elevation = 0.dp,
                onCloseRequest = { dismiss() },
                buttons = {
                    negativeButton(
                        res = R.string.search_online,
                        textStyle = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
                    ) {
                        webSearchDialogState.show()
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
                Box(
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
            StartWebSearchDialog(webSearchDialogState)
        }

    }


    class LastFmViewModel : ViewModel() {

        private lateinit var delgate: LastFmClientDelegate

        var mode: String? = null

        var target: Any? = null

        private val _response: MutableStateFlow<LastFmModel?> = MutableStateFlow(null)
        val response get() = _response.asStateFlow()

        fun prepareDelegate(context: Context) {
            delgate = LastFmClientDelegate(context, USER_AGENT, errorReporter, viewModelScope)
        }


        fun loadArtist(context: Context, artist: Artist) {
            viewModelScope.launch(Dispatchers.IO) {
                val target = ArtistResult.Artist(artist.name, "", emptyList(), null)

                var action: LastFmAction? = null
                var response: LastFmArtistResponse? = null

                action = LastFmAction.View.ViewArtist(target, Locale.getDefault().language)
                response = delgate.request(context, action).await() as? LastFmArtistResponse
                if (response == null) {
                    action = LastFmAction.View.ViewArtist(target, null)
                    response = delgate.request(context, action).await() as? LastFmArtistResponse
                }

                _response.update { response?.artist }
            }
        }

        fun loadAlbum(context: Context, album: Album) {
            viewModelScope.launch(Dispatchers.IO) {

                val target = AlbumResult.Album(album.title, album.artistName ?: "", "", emptyList(), null)

                var action: LastFmAction? = null
                var response: LastFmAlbumResponse? = null

                action = LastFmAction.View.ViewAlbum(target, Locale.getDefault().language)
                response = delgate.request(context, action).await() as? LastFmAlbumResponse
                if (response == null) {
                    action = LastFmAction.View.ViewAlbum(target, null)
                    response = delgate.request(context, action).await() as? LastFmAlbumResponse
                }

                _response.update { response?.album }
            }
        }


        fun loadSong(context: Context, song: Song) {
            viewModelScope.launch(Dispatchers.IO) {

                val target = TrackResult.Track(song.title, song.artistName ?: "", "", emptyList(), null)

                var action: LastFmAction? = null
                var response: LastFmTrackResponse? = null

                action = LastFmAction.View.ViewTrack(target, Locale.getDefault().language)
                response = delgate.request(context, action).await() as? LastFmTrackResponse
                if (response == null) {
                    action = LastFmAction.View.ViewTrack(target, null)
                    response = delgate.request(context, action).await() as? LastFmTrackResponse
                }

                _response.update { response?.track }
            }
        }

        companion object {
            private val errorReporter = object : AbsClientDelegate.ExceptionHandler {
                override fun reportError(e: Throwable, tag: String, message: String) =
                    player.phonograph.util.reportError(e, tag, message)

                override fun warning(tag: String, message: String) =
                    player.phonograph.util.warning(tag, message)

            }
        }

    }


    @Composable
    private fun StartWebSearchDialog(state: MaterialDialogState) {
        MaterialDialog(state) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    Source.LastFm.name,
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            startWebSearch(Source.LastFm)
                            state.hide()
                        }
                        .padding(vertical = 24.dp)
                )
                Text(
                    Source.MusicBrainz.name,
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            startWebSearch(Source.MusicBrainz)
                            state.hide()
                        }
                        .padding(vertical = 24.dp)
                )
            }
        }
    }

    private fun startWebSearch(mode: Source) {
        val context = requireContext()
        val intent = when (mode) {
            is Source.MusicBrainz -> when (viewModel.mode) {
                TYPE_ARTIST -> WebSearchLauncher.searchMusicBrainzArtist(context, viewModel.target as Artist)
                TYPE_ALBUM  -> WebSearchLauncher.searchMusicBrainzAlbum(context, viewModel.target as Album)
                TYPE_SONG   -> WebSearchLauncher.searchMusicBrainzSong(context, viewModel.target as Song)
                else        -> WebSearchLauncher.launchIntent(context)
            }

            is Source.LastFm      -> when (viewModel.mode) {
                TYPE_ARTIST -> WebSearchLauncher.searchLastFmArtist(context, viewModel.target as Artist)
                TYPE_ALBUM  -> WebSearchLauncher.searchLastFmAlbum(context, viewModel.target as Album)
                TYPE_SONG   -> WebSearchLauncher.searchLastFmSong(context, viewModel.target as Song)
                else        -> WebSearchLauncher.launchIntent(context)
            }
        }
        context.startActivity(intent)
        dismiss()
    }

    private sealed class Source(val name: String) {
        object MusicBrainz : Source("MusicBrainz")
        object LastFm : Source("last.fm")
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