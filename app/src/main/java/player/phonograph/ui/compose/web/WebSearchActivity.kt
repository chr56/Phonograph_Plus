/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.base.ComposeThemeActivity
import player.phonograph.ui.compose.base.Navigator
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.parcelableExtra
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.os.Bundle
import kotlinx.coroutines.launch
import player.phonograph.model.Album as PhonographAlbum
import player.phonograph.model.Artist as PhonographArtist
import player.phonograph.model.Song as PhonographSong

class WebSearchActivity : ComposeThemeActivity() {

    private val viewModel: WebSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkCommand(this, intent)

        setContent {

            val highlightColor by primaryColor.collectAsState()
            PhonographTheme(highlightColor) {
                val scaffoldState = rememberScaffoldState()
                val page by viewModel.navigator.currentPage.collectAsState()
                WebSearch(viewModel, scaffoldState, page)
            }
        }
    }

    override fun onBackPressed() {
        if (!viewModel.navigator.navigateUp()) super.onBackPressed()
    }

    private fun checkCommand(context: Context, intent: Intent) {
        when (intent.getStringExtra(EXTRA_COMMAND)) {
            EXTRA_QUERY_LASTFM_ALBUM              ->
                intent.parcelableExtra<PhonographAlbum>(EXTRA_DATA)?.let { queryFactory.lastFmQuery(context, it) }
                    .also {
                        prefillLastFmSearch(viewModel.navigator, it)
                    }

            EXTRA_QUERY_LASTFM_ARTIST             ->
                intent.parcelableExtra<PhonographArtist>(EXTRA_DATA)?.let { queryFactory.lastFmQuery(context, it) }
                    .also {
                        prefillLastFmSearch(viewModel.navigator, it)
                    }

            EXTRA_QUERY_LASTFM_SONG               ->
                intent.parcelableExtra<PhonographSong>(EXTRA_DATA)?.let { queryFactory.lastFmQuery(context, it) }.also {
                    prefillLastFmSearch(viewModel.navigator, it)
                }

            EXTRA_QUERY_MUSICBRAINZ_RELEASE_GROUP ->
                intent.parcelableExtra<PhonographAlbum>(EXTRA_DATA)
                    ?.let {
                        queryFactory.musicBrainzQuery(
                            context, MusicBrainzAction.Target.ReleaseGroup, luceneQuery(it)
                        )
                    }.also {
                        prefillMusicBrainzSearch(viewModel.navigator, it)
                    }

            EXTRA_QUERY_MUSICBRAINZ_RELEASE       ->
                intent.parcelableExtra<PhonographAlbum>(EXTRA_DATA)
                    ?.let {
                        queryFactory.musicBrainzQuery(
                            context, MusicBrainzAction.Target.Release, luceneQuery(it)
                        )
                    }.also {
                        prefillMusicBrainzSearch(viewModel.navigator, it)
                    }

            EXTRA_QUERY_MUSICBRAINZ_ARTIST        ->
                intent.parcelableExtra<PhonographArtist>(EXTRA_DATA)
                    ?.let {
                        queryFactory.musicBrainzQuery(
                            context, MusicBrainzAction.Target.Artist, it.name
                        )
                    }.also {
                        prefillMusicBrainzSearch(viewModel.navigator, it)
                    }

            EXTRA_QUERY_MUSICBRAINZ_RECORDING     ->
                intent.parcelableExtra<PhonographSong>(EXTRA_DATA)
                    ?.let {
                        queryFactory.musicBrainzQuery(
                            context, MusicBrainzAction.Target.Recording, luceneQuery(it)
                        )
                    }
                    .also {
                        prefillMusicBrainzSearch(viewModel.navigator, it)
                    }

            EXTRA_VIEW_MUSICBRAINZ_RELEASE_GROUP  ->
                parseMusicBrainzDetail(context, intent, viewModel) {
                    MusicBrainzAction.View(MusicBrainzAction.Target.ReleaseGroup, it)
                }

            EXTRA_VIEW_MUSICBRAINZ_RELEASE        ->
                parseMusicBrainzDetail(context, intent, viewModel) {
                    MusicBrainzAction.View(MusicBrainzAction.Target.Release, it)
                }

            EXTRA_VIEW_MUSICBRAINZ_ARTIST         ->
                parseMusicBrainzDetail(context, intent, viewModel) {
                    MusicBrainzAction.View(MusicBrainzAction.Target.Artist, it)
                }

            EXTRA_VIEW_MUSICBRAINZ_RECORDING      ->
                parseMusicBrainzDetail(context, intent, viewModel) {
                    MusicBrainzAction.View(MusicBrainzAction.Target.Recording, it)
                }


            else                                  -> null
        }

    }

    val queryFactory get() = viewModel.queryFactory

    companion object {
        const val EXTRA_COMMAND = "COMMAND"
        const val EXTRA_QUERY_LASTFM_SONG = "Query:LastFm:Song"
        const val EXTRA_QUERY_LASTFM_ARTIST = "Query:LastFm:Artist"
        const val EXTRA_QUERY_LASTFM_ALBUM = "Query:LastFm:Album"
        const val EXTRA_QUERY_MUSICBRAINZ_RELEASE_GROUP = "Query:MusicBrain:ReleaseGroup"
        const val EXTRA_QUERY_MUSICBRAINZ_RELEASE = "Query:MusicBrain:Release"
        const val EXTRA_QUERY_MUSICBRAINZ_ARTIST = "Query:MusicBrain:Artist"
        const val EXTRA_QUERY_MUSICBRAINZ_RECORDING = "Query:MusicBrain:Recording"
        const val EXTRA_VIEW_MUSICBRAINZ_RELEASE_GROUP = "view:MusicBrain:ReleaseGroup"
        const val EXTRA_VIEW_MUSICBRAINZ_RELEASE = "view:MusicBrain:Release"
        const val EXTRA_VIEW_MUSICBRAINZ_ARTIST = "view:MusicBrain:Artist"
        const val EXTRA_VIEW_MUSICBRAINZ_RECORDING = "view:MusicBrain:Recording"
        const val EXTRA_DATA = "DATA"

        private fun prefillLastFmSearch(navigator: Navigator<Page>, lastFmQuery: LastFmQuery?) {
            if (lastFmQuery != null) {
                val page = Page.Search.LastFmSearch(lastFmQuery)
                navigator.navigateTo(page)
            }
        }

        private fun prefillMusicBrainzSearch(navigator: Navigator<Page>, musicBrainzQuery: MusicBrainzQuery?) {
            if (musicBrainzQuery != null) {
                val page = Page.Search.MusicBrainzSearch(musicBrainzQuery)
                navigator.navigateTo(page)
            }
        }

        private fun parseMusicBrainzDetail(
            context: Context,
            intent: Intent,
            viewModel: WebSearchViewModel,
            process: (String) -> MusicBrainzAction,
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

        private fun luceneQuery(song: PhonographSong): String = buildString {
            append(""""${song.title}"""")
            if (!song.artistName.isNullOrEmpty()) append(""" AND artist:"${song.artistName}"""")
            if (!song.albumName.isNullOrEmpty()) append(""" AND release:"${song.albumName}"""")
        }

        private fun luceneQuery(album: PhonographAlbum): String = buildString {
            append(""""${album.title}"""")
            if (album.artistName.isNotEmpty()) append(""" AND artist:"${album.artistName}"""")
        }



        fun launchIntent(context: Context): Intent =
            Intent(context, WebSearchActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_DOCUMENT
            }

        fun searchLastFmAlbum(context: Context, data: PhonographAlbum?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_QUERY_LASTFM_ALBUM)
                putExtra(EXTRA_DATA, data)
            }

        fun searchLastFmArtist(context: Context, data: PhonographArtist?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_QUERY_LASTFM_ARTIST)
                putExtra(EXTRA_DATA, data)
            }

        fun searchLastFmSong(context: Context, data: PhonographSong?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_QUERY_LASTFM_SONG)
                putExtra(EXTRA_DATA, data)
            }

        fun searchMusicBrainzAlbum(context: Context, data: PhonographAlbum?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_QUERY_MUSICBRAINZ_RELEASE_GROUP)
                putExtra(EXTRA_DATA, data)
            }

        fun searchMusicBrainzArtist(context: Context, data: PhonographArtist?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_QUERY_MUSICBRAINZ_ARTIST)
                putExtra(EXTRA_DATA, data)
            }

        fun searchMusicBrainzSong(context: Context, data: PhonographSong?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_QUERY_MUSICBRAINZ_RECORDING)
                putExtra(EXTRA_DATA, data)
            }

        fun viewIntentMusicBrainzReleaseGroup(context: Context, mbid: String): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_VIEW_MUSICBRAINZ_RELEASE_GROUP)
                putExtra(EXTRA_DATA, mbid)
            }

        fun viewIntentMusicBrainzRelease(context: Context, mbid: String): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_VIEW_MUSICBRAINZ_RELEASE)
                putExtra(EXTRA_DATA, mbid)
            }

        fun viewIntentMusicBrainzArtist(context: Context, mbid: String): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_VIEW_MUSICBRAINZ_ARTIST)
                putExtra(EXTRA_DATA, mbid)
            }

        fun viewIntentMusicBrainzRecording(context: Context, mbid: String): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_COMMAND, EXTRA_VIEW_MUSICBRAINZ_RECORDING)
                putExtra(EXTRA_DATA, mbid)
            }
    }
}

