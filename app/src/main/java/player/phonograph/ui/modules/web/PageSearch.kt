/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import mms.QueryParameter
import mms.Source
import mms.lastfm.LastFmAction
import mms.lastfm.LastFmQueryParameter
import mms.musicbrainz.MusicBrainzAction
import mms.musicbrainz.MusicbrainzQueryParameter
import player.phonograph.R
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.content.Context

sealed class PageSearch<P : QueryParameter>(val source: Source) : Page(R.string.action_search) {
    override fun title(context: Context): String = "${super.title(context)} ${source.name}"

    abstract val queryParameter: P

    class LastFmSearch(
        albumQuery: String? = null,
        artistQuery: String? = null,
        trackQuery: String? = null,
        target: LastFmAction.Target = LastFmAction.Target.Album,
    ) : PageSearch<LastFmQueryParameter>(Source.LastFm) {
        var target by mutableStateOf(target)
        val albumState = TextFieldState(initialText = albumQuery.orEmpty())
        val artistState = TextFieldState(initialText = artistQuery.orEmpty())
        val trackState = TextFieldState(initialText = trackQuery.orEmpty())

        override val queryParameter: LastFmQueryParameter
            get() = LastFmQueryParameter(
                target,
                albumState.text.toString(),
                artistState.text.toString(),
                trackState.text.toString(),
            )
    }

    class MusicBrainzSearch(
        target: MusicBrainzAction.Target = MusicBrainzAction.Target.ReleaseGroup,
        query: String = "",
    ) : PageSearch<MusicbrainzQueryParameter>(Source.MusicBrainz) {
        var target by mutableStateOf(target)
        val queryState = TextFieldState(initialText = query)

        override val queryParameter: MusicbrainzQueryParameter
            get() = MusicbrainzQueryParameter(target, queryState.text.toString())
    }
}