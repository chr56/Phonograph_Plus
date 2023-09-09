/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.base.ComposeThemeActivity
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.ui.compose.web.WebSearchActionConst.LASTFM_SEARCH
import player.phonograph.ui.compose.web.WebSearchActionConst.MUSICBRAINZ_SEARCH
import player.phonograph.ui.compose.web.WebSearchActionConst.MUSICBRAINZ_VIEW
import player.phonograph.util.parcelableExtra
import util.phonograph.tagsources.musicbrainz.MusicBrainzModel
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

    val viewModel: WebSearchViewModel by viewModels()

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

        when (intent.getStringExtra(EXTRA_ACTION_TYPE)) {

            MUSICBRAINZ_SEARCH ->
                intent.parcelableExtra<MusicBrainzAction.Search>(EXTRA_DATA)?.also { action ->
                    val page = PageSearch.MusicBrainzSearch(action.target, action.query)
                    viewModel.navigator.navigateTo(page)
                }

            MUSICBRAINZ_VIEW   ->
                intent.parcelableExtra<MusicBrainzAction.View>(EXTRA_DATA)?.also { action ->
                    val clientDelegate = viewModel.clientDelegateMusicBrainz(context)
                    viewModel.viewModelScope.launch {
                        val result = clientDelegate.request(context, action).await()
                        val page = PageDetail.MusicBrainzDetail(result as? MusicBrainzModel)
                        viewModel.navigator.navigateTo(page)
                    }
                }

            LASTFM_SEARCH      ->
                intent.parcelableExtra<LastFmAction.Search>(EXTRA_ACTION_TYPE).also {
                    val page = PageSearch.LastFmSearch(
                        albumQuery = it?.album,
                        artistQuery = it?.artist,
                        trackQuery = it?.track,
                        target = it?.target ?: LastFmAction.Target.Album
                    )
                    viewModel.navigator.navigateTo(page)
                }
        }
    }

    companion object {
        const val EXTRA_ACTION_TYPE = "ACTION"
        const val EXTRA_DATA = "DATA"

        private fun luceneQuery(song: PhonographSong): String = buildString {
            append(""""${song.title}"""")
            if (!song.artistName.isNullOrEmpty()) append(""" AND artist:"${song.artistName}"""")
            if (!song.albumName.isNullOrEmpty()) append(""" AND release:"${song.albumName}"""")
        }

        private fun luceneQuery(album: PhonographAlbum): String = buildString {
            append(""""${album.title}"""")
            if (album.artistName.isNotEmpty()) append(""" AND artist:"${album.artistName}"""")
        }

        /**
         * default launch intent
         */
        fun launchIntent(context: Context): Intent =
            Intent(context, WebSearchActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_DOCUMENT
            }

        fun searchLastFmAlbum(context: Context, item: PhonographAlbum?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_ACTION_TYPE, LASTFM_SEARCH)
                putExtra(
                    EXTRA_DATA,
                    LastFmAction.Search(
                        LastFmAction.Target.Album,
                        album = item?.title.orEmpty(),
                        artist = item?.artistName.orEmpty()
                    )
                )
            }

        fun searchLastFmArtist(context: Context, item: PhonographArtist?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_ACTION_TYPE, LASTFM_SEARCH)
                putExtra(
                    EXTRA_DATA,
                    LastFmAction.Search(
                        LastFmAction.Target.Artist,
                        artist = item?.name.orEmpty()
                    )
                )
            }

        fun searchLastFmSong(context: Context, item: PhonographSong?): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_ACTION_TYPE, LASTFM_SEARCH)
                putExtra(
                    EXTRA_DATA,
                    LastFmAction.Search(
                        LastFmAction.Target.Track,
                        album = item?.albumName.orEmpty(),
                        artist = item?.artistName.orEmpty(),
                        track = item?.title.orEmpty()
                    )
                )
            }

        fun searchMusicBrainzAlbum(context: Context, item: PhonographAlbum?): Intent =
            launchIntent(context).apply {
                if (item != null) {
                    putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_SEARCH)
                    putExtra(
                        EXTRA_DATA,
                        MusicBrainzAction.Search(MusicBrainzAction.Target.ReleaseGroup, luceneQuery(item))
                    )
                }
            }

        fun searchMusicBrainzArtist(context: Context, item: PhonographArtist?): Intent =
            launchIntent(context).apply {
                if (item != null) {
                    putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_SEARCH)
                    putExtra(
                        EXTRA_DATA,
                        MusicBrainzAction.Search(MusicBrainzAction.Target.Artist, item.name)
                    )
                }
            }

        fun searchMusicBrainzSong(context: Context, item: PhonographSong?): Intent =
            launchIntent(context).apply {
                if (item != null) {
                    putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_SEARCH)
                    putExtra(
                        EXTRA_DATA,
                        MusicBrainzAction.Search(MusicBrainzAction.Target.Recording, luceneQuery(item))
                    )
                }
            }

        fun viewIntentMusicBrainzReleaseGroup(context: Context, mbid: String): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_VIEW)
                putExtra(
                    EXTRA_DATA, MusicBrainzAction.View(MusicBrainzAction.Target.ReleaseGroup, mbid)
                )
            }

        fun viewIntentMusicBrainzRelease(context: Context, mbid: String): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_VIEW)
                putExtra(
                    EXTRA_DATA, MusicBrainzAction.View(MusicBrainzAction.Target.Release, mbid)
                )
            }

        fun viewIntentMusicBrainzArtist(context: Context, mbid: String): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_VIEW)
                putExtra(
                    EXTRA_DATA, MusicBrainzAction.View(MusicBrainzAction.Target.Artist, mbid)
                )
            }

        fun viewIntentMusicBrainzRecording(context: Context, mbid: String): Intent =
            launchIntent(context).apply {
                putExtra(EXTRA_ACTION_TYPE, MUSICBRAINZ_VIEW)
                putExtra(
                    EXTRA_DATA, MusicBrainzAction.View(MusicBrainzAction.Target.Recording, mbid)
                )
            }
    }
}

