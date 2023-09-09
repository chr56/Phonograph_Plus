/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class PageSearch<Q : Query<*, *>>(q: Q, val source: String) : Page(R.string.action_search) {
    override fun title(context: Context): String = "${super.title(context)} $source"

    private val _query: MutableStateFlow<Q> = MutableStateFlow(q)
    val query get() = _query.asStateFlow()

    class LastFmSearch(q: LastFmQuery) : PageSearch<LastFmQuery>(q, "Last.FM")
    class MusicBrainzSearch(q: MusicBrainzQuery) : PageSearch<MusicBrainzQuery>(q, "MusicBrainz")
}