/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import player.phonograph.R
import player.phonograph.ui.compose.components.HorizontalTextItem
import util.phonograph.tagsources.lastfm.LastFmAction
import util.phonograph.tagsources.lastfm.LastFmQueryParameter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource


@Composable
fun LastFmSearchBox(
    queryParameter: LastFmQueryParameter,
    updateQueryParameter: ((LastFmQueryParameter) -> LastFmQueryParameter) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: (LastFmAction.Search) -> Unit,
) {
    BaseSearchBox(
        modifier,
        title = "last.fm",
        target = {
            Target(
                all = listOf(LastFmAction.Target.Album, LastFmAction.Target.Artist, LastFmAction.Target.Track),
                text = { stringResource(it.displayTextRes) },
                current = queryParameter.target
            ) {
                updateQueryParameter { old -> old.copy(target = it) }
            }
        },
        onSearch = { onSearch(queryParameter.toAction()) }
    ) {
        if (queryParameter.target == LastFmAction.Target.Album)
            HorizontalTextItem(name = stringResource(id = R.string.album)) {
                SearchTextBox(queryParameter.albumQuery.orEmpty()) {
                    updateQueryParameter { old ->
                        old.copy(albumQuery = it)
                    }
                }
            }
        if (queryParameter.target == LastFmAction.Target.Track)
            HorizontalTextItem(name = stringResource(id = R.string.song)) {
                SearchTextBox(queryParameter.trackQuery.orEmpty()) {
                    updateQueryParameter { old ->
                        old.copy(trackQuery = it)
                    }
                }
            }
        if (queryParameter.target == LastFmAction.Target.Artist || queryParameter.target == LastFmAction.Target.Track)
            HorizontalTextItem(name = stringResource(id = R.string.artist)) {
                SearchTextBox(queryParameter.artistQuery.orEmpty()) {
                    updateQueryParameter { old ->
                        old.copy(artistQuery = it)
                    }
                }
            }
    }
}
