/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import mms.lastfm.LastFmAction
import player.phonograph.R
import player.phonograph.ui.compose.components.display.HorizontalTextItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource


@Composable
fun LastFmSearchBox(
    page: PageSearch.LastFmSearch,
    modifier: Modifier = Modifier,
    onSearch: (LastFmAction.Search) -> Unit,
) {
    BaseSearchBox(
        modifier,
        title = "last.fm",
        target = {
            Target(
                all = listOf(LastFmAction.Target.Album, LastFmAction.Target.Artist, LastFmAction.Target.Track),
                text = { stringResource(it.displayTextRes()) },
                current = page.target
            ) {
                page.target = it
            }
        },
        onSearch = { onSearch(page.queryParameter.toAction()) }
    ) {
        if (page.target == LastFmAction.Target.Album)
            HorizontalTextItem(label = stringResource(id = R.string.label_album)) {
                SearchTextBox(state = page.albumState)
            }
        if (page.target == LastFmAction.Target.Track)
            HorizontalTextItem(label = stringResource(id = R.string.label_song)) {
                SearchTextBox(state = page.trackState)
            }
        if (page.target == LastFmAction.Target.Artist || page.target == LastFmAction.Target.Track)
            HorizontalTextItem(label = stringResource(id = R.string.label_artist)) {
                SearchTextBox(state = page.artistState)
            }
    }
}
