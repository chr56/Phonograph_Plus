/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
fun LastFmSearchBox(lastFmQuery: LastFmQuery, modifier: Modifier = Modifier, onSearch: (LastFmQuery.QueryAction) -> Unit) {
    Column(modifier.padding(vertical = 8.dp)) {
        val target by lastFmQuery.target.collectAsState()
        Text(
            text = "last.fm",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.h5
        )
        Line(name = "Target") {
            Target(
                all = listOf(LastFmQuery.Target.Release, LastFmQuery.Target.Artist, LastFmQuery.Target.Track),
                current = target
            ) {
                lastFmQuery.target.tryEmit(it)
            }
        }
        if (target == LastFmQuery.Target.Release)
            Line(name = stringResource(id = R.string.album)) {
                val current by lastFmQuery.releaseQuery.collectAsState()
                TextBox(current.orEmpty()) { lastFmQuery.releaseQuery.tryEmit(it) }
            }
        if (target == LastFmQuery.Target.Track)
            Line(name = stringResource(id = R.string.song)) {
                val current by lastFmQuery.trackQuery.collectAsState()
                TextBox(current.orEmpty()) { lastFmQuery.trackQuery.tryEmit(it) }
            }
        if (target == LastFmQuery.Target.Artist || target == LastFmQuery.Target.Track)
            Line(name = stringResource(id = R.string.artist)) {
                val current by lastFmQuery.artistQuery.collectAsState()
                TextBox(current.orEmpty()) { lastFmQuery.artistQuery.tryEmit(it) }
            }
        TextButton(
            onClick = {
                onSearch(lastFmQuery.searchAction())
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
private fun Line(name: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            name,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(2f)
                .align(Alignment.CenterVertically)
        )
        Box(
            Modifier
                .weight(9f)
        ) {
            content()
        }
    }
}


@Composable
private fun Target(
    all: Collection<LastFmQuery.Target>,
    current: LastFmQuery.Target,
    modifier: Modifier = Modifier,
    onUpdate: (LastFmQuery.Target) -> Unit,
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
                    Modifier
                        .align(Alignment.CenterVertically)
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