/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource


@Composable
fun LastFmSearchBox(
    lastFmQuery: LastFmQuery,
    modifier: Modifier = Modifier,
    onSearch: (LastFmAction.Search) -> Unit,
) {
    val queryParameter by lastFmQuery.queryParameter.collectAsState()
    BaseSearchBox(
        modifier,
        title = "last.fm",
        target = {
            Target(
                all = listOf(LastFmAction.Target.Album, LastFmAction.Target.Artist, LastFmAction.Target.Track),
                text = { it.displayName },
                current = queryParameter.target
            ) {
                lastFmQuery.updateQueryParameter { old -> old.copy(target = it) }
            }
        },
        onSearch = { onSearch(lastFmQuery.searchAction()) }
    ) {
        if (queryParameter.target == LastFmAction.Target.Album)
            Line(name = stringResource(id = R.string.album)) {
                SearchTextBox(queryParameter.albumQuery.orEmpty()) {
                    lastFmQuery.updateQueryParameter { old ->
                        old.copy(albumQuery = it)
                    }
                }
            }
        if (queryParameter.target == LastFmAction.Target.Track)
            Line(name = stringResource(id = R.string.song)) {
                SearchTextBox(queryParameter.trackQuery.orEmpty()) {
                    lastFmQuery.updateQueryParameter { old ->
                        old.copy(trackQuery = it)
                    }
                }
            }
        if (queryParameter.target == LastFmAction.Target.Artist || queryParameter.target == LastFmAction.Target.Track)
            Line(name = stringResource(id = R.string.artist)) {
                SearchTextBox(queryParameter.artistQuery.orEmpty()) {
                    lastFmQuery.updateQueryParameter { old ->
                        old.copy(artistQuery = it)
                    }
                }
            }
    }
}
