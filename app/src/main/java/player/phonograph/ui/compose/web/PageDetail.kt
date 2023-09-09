/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import util.phonograph.tagsources.lastfm.LastFmModel
import util.phonograph.tagsources.musicbrainz.MusicBrainzModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class PageDetail<T>(data: T?) : Page(R.string.label_details) {

    private val _detail: MutableStateFlow<T?> = MutableStateFlow(data)
    val detail get() = _detail.asStateFlow()

    class LastFmDetail(result: LastFmModel?) : PageDetail<LastFmModel>(result)
    class MusicBrainzDetail(result: MusicBrainzModel?) : PageDetail<MusicBrainzModel>(result)
}