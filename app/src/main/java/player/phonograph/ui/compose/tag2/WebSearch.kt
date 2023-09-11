/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import player.phonograph.R
import player.phonograph.ui.compose.web.WebSearchTool
import util.phonograph.tagsources.Source
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun RequestWebSearch(
    webSearchTool: WebSearchTool,
    onSearch: (Source) -> Unit,
    onShowWikiDialog: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = !expanded }) {
        Icon(painterResource(id = R.drawable.ic_search_white_24dp), null)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(onClick = { onSearch(Source.MusicBrainz) }
        ) {
            Text(Source.MusicBrainz.name, Modifier.padding(8.dp))
        }
        DropdownMenuItem(onClick = { onSearch(Source.LastFm) }
        ) {
            Text(Source.LastFm.name, Modifier.padding(8.dp))
        }
        if (onShowWikiDialog != null) {
            DropdownMenuItem(onClick = {
                onShowWikiDialog.invoke()
            }) {
                Text(stringResource(R.string.wiki), Modifier.padding(8.dp))
            }
        }
    }
}