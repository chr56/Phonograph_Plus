/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.components.HorizontalTextItem
import util.phonograph.tagsources.musicbrainz.MusicBrainzAction
import util.phonograph.tagsources.musicbrainz.MusicBrainzAction.Target
import util.phonograph.tagsources.musicbrainz.MusicbrainzQueryParameter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


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
                text = { it.name },
                current = queryParameter.target
            ) {
                updateQueryParameter { old -> old.copy(target = it) }
            }
        },
        onSearch = { onSearch(queryParameter.toAction()) }
    ) {
        HorizontalTextItem(name = "Query") {
            SearchTextBox(
                queryParameter.query,
                hint = "Lucene query syntax is supported!"
            ) {
                updateQueryParameter { old -> old.copy(query = it) }
            }
        }
    }
}
