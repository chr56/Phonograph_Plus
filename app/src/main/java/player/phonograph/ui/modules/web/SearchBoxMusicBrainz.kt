/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import mms.musicbrainz.MusicBrainzAction
import mms.musicbrainz.MusicBrainzAction.Target
import mms.musicbrainz.MusicbrainzQueryParameter
import player.phonograph.R
import player.phonograph.ui.compose.components.HorizontalTextItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource


@Composable
fun MusicBrainzSearchBox(
    queryParameter: MusicbrainzQueryParameter,
    updateQueryParameter: ((MusicbrainzQueryParameter) -> MusicbrainzQueryParameter) -> Unit,
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
                current = queryParameter.target
            ) {
                updateQueryParameter { old -> old.copy(target = it) }
            }
        },
        onSearch = { onSearch(queryParameter.toAction()) }
    ) {
        HorizontalTextItem(stringResource(R.string.label_query)) {
            SearchTextBox(
                queryParameter.query,
                hint = stringResource(R.string.tips_hint_lucene_query_syntax)
            ) {
                updateQueryParameter { old -> old.copy(query = it) }
            }
        }
    }
}
