/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import org.jaudiotagger.tag.FieldKey
import player.phonograph.R
import player.phonograph.model.songTagNameRes
import player.phonograph.ui.compose.components.Title
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun DiffScreen(model: TagEditorModel) {
    val diff = remember { model.infoTableViewModel.generateDiff() }
    if (diff.isEmpty())
        Text(text = stringResource(id = R.string.no_changes))
    else
        LazyColumn(Modifier.padding(8.dp)) {
            for (tag in diff) {
                item {
                    Diff(tag)
                }
            }
        }
}

@Composable
private fun Diff(tag: Triple<FieldKey, String?, String?>) {
    Column(Modifier.padding(vertical = 16.dp)) {
        Title(stringResource(id = songTagNameRes(tag.first)), horizontalPadding = 0.dp)
        DiffText(tag.second)
        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        DiffText(tag.third)
    }
}

@Composable
private fun DiffText(string: String?, modifier: Modifier = Modifier) {
    if (string.isNullOrEmpty()) {
        Text(
            stringResource(id = R.string.empty),
            modifier
                .fillMaxWidth()
                .alpha(0.5f)
        )
    } else {
        Text(string, modifier.fillMaxWidth())
    }
}