/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.components

import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.ui.compose.components.CascadeVerticalItem
import player.phonograph.ui.compose.components.VerticalTextItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun FileItems(songs: List<Song>) {
    CascadeVerticalItem(
        stringResource(R.string.label_files),
        collapsible = true,
        collapsed = false
    ) {
        for ((index, song) in songs.withIndex()) {
            Row(modifier = Modifier.fillMaxWidth(), Arrangement.Start) {
                Box(
                    Modifier
                        .align(Alignment.Top)
                        .padding(top = 8.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_music_note_white_24dp), null
                    )
                    Text(
                        "${index + 1}",
                        Modifier.align(Alignment.BottomEnd),
                        style = MaterialTheme.typography.overline
                    )
                }
                Column {
                    VerticalTextItem(
                        stringResource(R.string.label_title),
                        song.title
                    )
                    VerticalTextItem(
                        stringResource(R.string.label_file_path),
                        song.data
                    )
                }
            }
        }
    }
}