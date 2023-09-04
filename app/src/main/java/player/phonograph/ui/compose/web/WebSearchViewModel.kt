/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import androidx.lifecycle.ViewModel
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebSearchViewModel : ViewModel() {

    sealed class Page {
        object Search : Page()
        object Detail : Page()
    }

    private val _page: MutableStateFlow<Page> = MutableStateFlow(Page.Search)
    val page get() = _page.asStateFlow()

    fun updatePage(page: Page) {
        _page.value = page
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
    }
}
