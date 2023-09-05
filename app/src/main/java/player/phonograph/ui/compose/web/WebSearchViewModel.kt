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
        object Home : Page(R.string.intro_label)

        sealed class Search<Q : Query<*, *>>(q: Q, val source: String) : Page(R.string.action_search) {

            private val _query: MutableStateFlow<Q> = MutableStateFlow(q)
            val query get() = _query.asStateFlow()

            class LastFmSearch(q: LastFmQuery) : Search<LastFmQuery>(q, "Last.FM")
            class MusicBrainzSearch(q: MusicBrainzQuery) : Search<MusicBrainzQuery>(q, "MusicBrainz")
        }

        sealed class Detail(data: Any?) : Page(R.string.label_details) {

            private val _detail: MutableStateFlow<Any?> = MutableStateFlow(data)
            val detail get() = _detail.asStateFlow()

            class LastFmDetail(result: Any) : Detail(result)
            class MusicBrainzDetail(result: Any) : Detail(result)
        }

        companion object {
            val RootRage: Page = Home
        }

        fun isRoot() = equals(Page.RootRage)
    }

    val navigator = Navigator()

    class Navigator {

        private val _pages: MutableList<Page> = mutableListOf(Page.RootRage)
        val pages get() = _pages.toList()

        private val _currentPage: MutableStateFlow<Page> = MutableStateFlow(Page.RootRage)
        val currentPage get() = _currentPage.asStateFlow()

        fun navigateTo(page: Page) {
            _pages.add(page)
            _currentPage.value = page
        }

        /**
         * @return false if reaching to root
         */
        fun navigateUp(level: Int = 1): Boolean {
            if (level < 1 || level >= _pages.size) return true
            repeat(level) { _pages.removeLastOrNull() }
            val last = _pages.lastOrNull()
            return if (last != null) {
                _currentPage.value = last
                true
            } else {
                false
            }
        }

    }

    val queryFactory = QueryFactory()

    inner class QueryFactory {

        fun lastFm(context: Context): LastFmQuery = LastFmQuery(context, this@WebSearchViewModel)

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
