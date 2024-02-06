/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import mms.lastfm.LastFmModel
import mms.musicbrainz.MusicBrainzModel
import player.phonograph.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class PageDetail<T>(data: T?) : Page(R.string.label_details) {

    private val _detail: MutableStateFlow<T?> = MutableStateFlow(data)
    val detail get() = _detail.asStateFlow()

    class LastFmDetail(result: LastFmModel?) : PageDetail<LastFmModel>(result)
    class MusicBrainzDetail(result: MusicBrainzModel?) : PageDetail<MusicBrainzModel>(result)
}