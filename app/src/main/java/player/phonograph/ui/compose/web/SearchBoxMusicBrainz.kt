/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.web.MusicBrainzAction.Target
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier


@Composable
fun MusicBrainzSearchBox(
    modifier: Modifier = Modifier,
    musicBrainzQuery: MusicBrainzQuery,
    onSearch: (MusicBrainzAction.Search) -> Unit,
) {
    val queryParameter by musicBrainzQuery.queryParameter.collectAsState()
    BaseSearchBox(
        modifier = modifier,
        title = "MusicBrainz",
        target = {
            Target(
                all = listOf(Target.ReleaseGroup, Target.Release, Target.Artist, Target.Recording),
                text = { it.name },
                current = queryParameter.target
            ) {
                musicBrainzQuery.updateQueryParameter { old -> old.copy(target = it) }
            }
        },
        onSearch = { onSearch(queryParameter.toAction()) }
    ) {
        Line(name = "Query") {
            SearchTextBox(
                queryParameter.query,
                hint = "Lucene query syntax is supported!"
            ) {
                musicBrainzQuery.updateQueryParameter { old ->
                    old.copy(query = it)
                }
            }
        }
    }
}
