/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class PageSearch<P : QueryParameter>(val source: Source) : Page(R.string.action_search) {
    override fun title(context: Context): String = "${super.title(context)} ${source.name}"

    abstract val queryParameter: StateFlow<P>
    abstract fun updateQueryParameter(update: (P) -> P)

    class LastFmSearch(
        albumQuery: String? = null,
        artistQuery: String? = null,
        trackQuery: String? = null,
        target: LastFmAction.Target = LastFmAction.Target.Album,
    ) : PageSearch<LastFmQueryParameter>(Source.LastFm) {
        private val _queryParameter: MutableStateFlow<LastFmQueryParameter> =
            MutableStateFlow(LastFmQueryParameter(target, albumQuery, artistQuery, trackQuery))
        override val queryParameter get() = _queryParameter.asStateFlow()
        override fun updateQueryParameter(update: (LastFmQueryParameter) -> LastFmQueryParameter) {
            _queryParameter.update(update)
        }
    }

    class MusicBrainzSearch(
        target: MusicBrainzAction.Target = MusicBrainzAction.Target.ReleaseGroup,
        query: String = "",
    ) : PageSearch<MusicbrainzQueryParameter>(Source.MusicBrainz) {
        private val _queryParameter: MutableStateFlow<MusicbrainzQueryParameter> =
            MutableStateFlow(MusicbrainzQueryParameter(target, query))
        override val queryParameter = _queryParameter.asStateFlow()
        override fun updateQueryParameter(update: (MusicbrainzQueryParameter) -> MusicbrainzQueryParameter) {
            _queryParameter.update(update)
        }
    }
}