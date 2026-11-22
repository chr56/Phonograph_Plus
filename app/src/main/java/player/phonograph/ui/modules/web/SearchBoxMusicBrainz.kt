/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import mms.musicbrainz.MusicBrainzAction
import mms.musicbrainz.MusicBrainzAction.Target
import player.phonograph.R
import player.phonograph.ui.compose.components.display.HorizontalTextItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource


@Composable
fun MusicBrainzSearchBox(
    page: PageSearch.MusicBrainzSearch,
    modifier: Modifier = Modifier,
    onSearch: (MusicBrainzAction.Search) -> Unit,
) {
    BaseSearchBox(
        modifier = modifier,
        title = "MusicBrainz",
        target = {
            Target(
                all = listOf(Target.ReleaseGroup, Target.Release, Target.Artist, Target.Recording),
                text = { stringResource(it.displayTextRes()) },
                current = page.target
            ) {
                page.target = it
            }
        },
        onSearch = { onSearch(page.queryParameter.toAction()) }
    ) {
        HorizontalTextItem(stringResource(R.string.label_query)) {
            SearchTextBox(
                state = page.queryState,
                hint = stringResource(R.string.tips_hint_lucene_query_syntax)
            )
        }
    }
}
