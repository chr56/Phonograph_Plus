/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebSearchViewModel : ViewModel() {

    sealed class Page(@StringRes val nameRes: Int) {
        object Search : Page(R.string.action_search)
        object Detail : Page(R.string.label_details)
        companion object {
            val RootRage: Page = Search
        }
        fun isRoot() = equals(Page.RootRage)
    }

    val navigator = Navigator()

    class Navigator {

        private val _page: MutableStateFlow<Page> = MutableStateFlow(Page.RootRage)
        val page get() = _page.asStateFlow()

        fun navigateTo(page: Page) {
            _page.value = page
        }

        fun navigateUp(): Boolean {
            return if (!_page.value.isRoot()) {
                navigateTo(Page.RootRage)
                true
            } else false
        }

    }

    private val _query: MutableStateFlow<Query<*, *>?> = MutableStateFlow(null)
    val query get() = _query.asStateFlow()

    fun prepareQuery(context: Context, query: Query<*, *>?) {
        _query.tryEmit(query ?: queryFactory.default(context))
    }

    val queryFactory = QueryFactory()

    inner class QueryFactory {

        fun default(context: Context): LastFmQuery = LastFmQuery(context, this@WebSearchViewModel)

        fun from(context: Context, artist: Artist): LastFmQuery =
            LastFmQuery(
                context, this@WebSearchViewModel,
                artistQuery = artist.name,
                target = LastFmQuery.Target.Artist
            )

        fun from(context: Context, album: Album): LastFmQuery =
            LastFmQuery(
                context, this@WebSearchViewModel,
                releaseQuery = album.title,
                artistQuery = album.artistName,
                target = LastFmQuery.Target.Release
            )

        fun from(context: Context, song: Song): LastFmQuery =
            LastFmQuery(
                context, this@WebSearchViewModel,
                releaseQuery = song.albumName,
                artistQuery = song.artistName,
                trackQuery = song.title,
                target = LastFmQuery.Target.Track
            )

        fun musicBrainzQuery(context: Context): MusicBrainzQuery =
            MusicBrainzQuery(context, this@WebSearchViewModel)

        fun musicBrainzQueryReleaseGroup(context: Context, mbid: String): MusicBrainzQuery =
            musicBrainzQuery(context).also { it.query(context, MusicBrainzQuery.QueryAction.ViewReleaseGroup(mbid)) }

        fun musicBrainzQueryRelease(context: Context, mbid: String): MusicBrainzQuery =
            musicBrainzQuery(context).also { it.query(context, MusicBrainzQuery.QueryAction.ViewRelease(mbid)) }

        fun musicBrainzQueryArtist(context: Context, mbid: String): MusicBrainzQuery =
            musicBrainzQuery(context).also { it.query(context, MusicBrainzQuery.QueryAction.ViewArtist(mbid)) }

        fun musicBrainzQueryRecording(context: Context, mbid: String): MusicBrainzQuery =
            musicBrainzQuery(context).also { it.query(context, MusicBrainzQuery.QueryAction.ViewRecording(mbid)) }
    }
}
