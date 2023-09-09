/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class PageDetail(data: Any?) : Page(R.string.label_details) {

    private val _detail: MutableStateFlow<Any?> = MutableStateFlow(data)
    val detail get() = _detail.asStateFlow()

    class LastFmDetail(result: Any) : PageDetail(result)
    class MusicBrainzDetail(result: Any) : PageDetail(result)
}