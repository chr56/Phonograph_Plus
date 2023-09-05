/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun MusicBrainzSearchBox(
    modifier: Modifier = Modifier,
    musicBrainzQuery: MusicBrainzQuery,
    onSearch: (MusicBrainzQuery.QueryAction) -> Unit,
) {
    Column(modifier.padding(vertical = 8.dp)) {
        val queryParameter by musicBrainzQuery.queryParameter.collectAsState()
        Text(
            text = "MusicBrainz",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.h5
        )
        Line(name = "Target") {
            Target(
                all = listOf(
                    MusicBrainzQuery.Target.ReleaseGroup,
                    MusicBrainzQuery.Target.Release,
                    MusicBrainzQuery.Target.Artist,
                    MusicBrainzQuery.Target.Recording
                ),
                current = queryParameter.target
            ) {
                musicBrainzQuery.updateQueryParameter { old ->
                    old.copy(target = it)
                }
            }
        }
        Line(name = "Query") {
            TextBox(queryParameter.query) {
                musicBrainzQuery.updateQueryParameter { old ->
                    old.copy(query = it)
                }
            }
        }
        TextButton(
            onClick = {
                onSearch(queryParameter.searchAction())
            },
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .align(Alignment.End)
        ) {
            Text(stringResource(R.string.action_search))
        }
    }
}

@Composable
private fun Target(
    all: Collection<MusicBrainzQuery.Target>,
    current: MusicBrainzQuery.Target,
    modifier: Modifier = Modifier,
    onUpdate: (MusicBrainzQuery.Target) -> Unit,
) {
    Row(modifier) {
        for (target in all) {
            Row(
                Modifier
                    .weight(1f)
                    .clickable { onUpdate(target) }
            ) {
                RadioButton(selected = target == current, {})
                Text(
                    target.name,
                    Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}


@Composable
private fun TextBox(current: String, modifier: Modifier = Modifier, onUpdate: (String) -> Unit) {
    TextField(
        value = current,
        onValueChange = onUpdate,
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MaterialTheme.colors.background,
            textColor = MaterialTheme.colors.onBackground,
        ),
        trailingIcon = {
            Icon(
                Icons.Default.Clear,
                contentDescription = stringResource(id = R.string.reset_action),
                modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        onUpdate.invoke("")
                    }
            )
        }
    )
}