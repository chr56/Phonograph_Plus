/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import player.phonograph.ui.compose.base.Navigator
import androidx.annotation.StringRes
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class Page(@StringRes val nameRes: Int) : Navigator.IPage {
    object Home : Page(R.string.intro_label)

    sealed class Search<Q : Query<*, *>>(q: Q, val source: String) : Page(R.string.action_search) {
        override fun title(context: Context): String = "${super.title(context)} $source"

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

    open fun title(context: Context): String = context.getString(nameRes)

    fun isRoot() = equals(Page.RootRage)
}

