/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import lib.phonograph.activity.ThemeActivity
import player.phonograph.ui.compose.base.Navigator
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.parcelableExtra
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.os.Bundle
import kotlinx.coroutines.launch
import player.phonograph.model.Album as PhonographAlbum
import player.phonograph.model.Artist as PhonographArtist
import player.phonograph.model.Song as PhonographSong

class WebSearchActivity : ThemeActivity() {

    private val viewModel: WebSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.useCustomStatusBar = false
        super.onCreate(savedInstanceState)

        checkCommand(this, intent)

        setContent {
            PhonographTheme {

                val scaffoldState = rememberScaffoldState()
                val page by viewModel.navigator.currentPage.collectAsState()

                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = {
                        TopAppBar(
                            title = { Text(page.title(LocalContext.current)) },
                            navigationIcon = {
                                Box(Modifier.padding(16.dp)) {
                                    NavigateButton(scaffoldState.drawerState, viewModel.navigator)
                                }
                            }
                        )
                    },
                    drawerContent = {
                        Drawer(viewModel)
                    }
                ) {
                    CompositionLocalProvider(LocalPageNavigator provides viewModel.navigator) {
                        Box(
                            modifier = Modifier
                                .padding(it)
                                .fillMaxWidth()
                        ) {
                            when (val p = page) {
                                Page.Home                        -> Home(viewModel, page)
                                is Page.Search.LastFmSearch      -> LastFmSearch(viewModel, p)
                                is Page.Search.MusicBrainzSearch -> MusicBrainzSearch(viewModel, p)
                                is Page.Detail.LastFmDetail      -> DetailLastFm(viewModel, p)
                                is Page.Detail.MusicBrainzDetail -> DetailMusicBrainz(viewModel, p)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (!viewModel.navigator.navigateUp()) super.onBackPressed()
    }

    private fun checkCommand(context: Context, intent: Intent) {
        val query = when (intent.getStringExtra(EXTRA_COMMAND)) {
            EXTRA_QUERY_ALBUM                    ->
                intent.parcelableExtra<PhonographAlbum>(EXTRA_DATA)?.let { queryFactory.from(context, it) }.also {
                    prefillLastFmSearch(viewModel.navigator, it)
                }

            EXTRA_QUERY_ARTIST                   ->
                intent.parcelableExtra<PhonographArtist>(EXTRA_DATA)?.let { queryFactory.from(context, it) }.also {
                    prefillLastFmSearch(viewModel.navigator, it)
                }

            EXTRA_QUERY_SONG                     ->
                intent.parcelableExtra<PhonographSong>(EXTRA_DATA)?.let { queryFactory.from(context, it) }.also {
                    prefillLastFmSearch(viewModel.navigator, it)
                }

            EXTRA_VIEW_MUSICBRAINZ_RELEASE_GROUP ->
                parseMusicBrainzDetail(context, intent, viewModel) {
                    MusicBrainzQuery.QueryAction.ViewReleaseGroup(it)
                }

            EXTRA_VIEW_MUSICBRAINZ_RELEASE       ->
                parseMusicBrainzDetail(context, intent, viewModel) {
                    MusicBrainzQuery.QueryAction.ViewRelease(it)
                }

            EXTRA_VIEW_MUSICBRAINZ_ARTIST        ->
                parseMusicBrainzDetail(context, intent, viewModel) {
                    MusicBrainzQuery.QueryAction.ViewArtist(it)
                }

            EXTRA_VIEW_MUSICBRAINZ_RECORDING     ->
                parseMusicBrainzDetail(context, intent, viewModel) {
                    MusicBrainzQuery.QueryAction.ViewRecording(it)
                }


            else                                 -> null
        }

    }

    val queryFactory get() = viewModel.queryFactory

    companion object {
        const val EXTRA_COMMAND = "COMMAND"
        const val EXTRA_QUERY_SONG = "Query:Song"
        const val EXTRA_QUERY_ARTIST = "Query:Artist"
        const val EXTRA_QUERY_ALBUM = "Query:Album"
        const val EXTRA_VIEW_MUSICBRAINZ_RELEASE_GROUP = "VIEW:MusicBrain:ReleaseGroup"
        const val EXTRA_VIEW_MUSICBRAINZ_RELEASE = "VIEW:MusicBrain:Release"
        const val EXTRA_VIEW_MUSICBRAINZ_ARTIST = "VIEW:MusicBrain:Artist"
        const val EXTRA_VIEW_MUSICBRAINZ_RECORDING = "VIEW:MusicBrain:Recording"
        const val EXTRA_DATA = "DATA"

        private fun prefillLastFmSearch(navigator: Navigator<Page>, lastFmQuery: LastFmQuery?) {
            if (lastFmQuery != null) {
                val page = Page.Search.LastFmSearch(lastFmQuery)
                navigator.navigateTo(page)
            }
        }

        private fun parseMusicBrainzDetail(
            context: Context,
            intent: Intent,
            viewModel: WebSearchViewModel,
            process: (String) -> MusicBrainzQuery.QueryAction,
        ): MusicBrainzQuery {
            val mbid = intent.getStringExtra(EXTRA_DATA).orEmpty()
            return viewModel.queryFactory.musicBrainzQuery(context).also {
                viewModel.viewModelScope.launch {
                    val result = it.query(context, process(mbid)).await()
                    val page = Page.Detail.MusicBrainzDetail(result ?: Any())
                    viewModel.navigator.navigateTo(page)
                }
            }
        }

        fun launchIntent(context: Context, data: PhonographAlbum?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_QUERY_ALBUM)
                putExtra(EXTRA_DATA, data)
            }

        fun launchIntent(context: Context, data: PhonographArtist?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_QUERY_ARTIST)
                putExtra(EXTRA_DATA, data)
            }

        fun launchIntent(context: Context, data: PhonographSong?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_QUERY_SONG)
                putExtra(EXTRA_DATA, data)
            }

        fun launchIntentMusicBrainzReleaseGroup(context: Context, mbid: String): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_VIEW_MUSICBRAINZ_RELEASE_GROUP)
                putExtra(EXTRA_DATA, mbid)
            }

        fun launchIntentMusicBrainzRelease(context: Context, mbid: String): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_VIEW_MUSICBRAINZ_RELEASE)
                putExtra(EXTRA_DATA, mbid)
            }

        fun launchIntentMusicBrainzArtist(context: Context, mbid: String): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_VIEW_MUSICBRAINZ_ARTIST)
                putExtra(EXTRA_DATA, mbid)
            }

        fun launchIntentMusicBrainzRecording(context: Context, mbid: String): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_VIEW_MUSICBRAINZ_RECORDING)
                putExtra(EXTRA_DATA, mbid)
            }

        fun launchIntent(context: Context): Intent =
            Intent(context, WebSearchActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_DOCUMENT
            }
    }
}

