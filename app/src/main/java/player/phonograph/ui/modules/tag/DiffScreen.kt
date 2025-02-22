/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import player.phonograph.R
import player.phonograph.mechanism.metadata.edit.EditAction
import player.phonograph.mechanism.metadata.edit.text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp



internal class TagDiff(
    /**
     * <EditAction, oldValue> pair
     */
    val tagDiff: List<Pair<EditAction, String?>>,
) {
    fun noChange() = tagDiff.isEmpty()
}


@Composable
internal fun DiffScreen(diff: TagDiff) {
    if (diff.noChange())
        Text(text = stringResource(id = R.string.no_changes))
    else
        LazyColumn(Modifier.padding(8.dp)) {
            for (tag in diff.tagDiff) {
                item {
                    TagDiff(tag)
                }
            }
        }
}

@Composable
private fun TagDiff(tag: Pair<EditAction, String?>) {
    val (action, old) = tag
    Column(Modifier.padding(vertical = 16.dp)) {
        Title(action.key.text(LocalContext.current.resources), horizontalPadding = 0.dp)
        DiffText(old)
        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        DiffText(
            when (action) {
                is EditAction.Update       -> action.newValue
                is EditAction.Delete       -> stringResource(id = R.string.empty)
                EditAction.ImageDelete     -> stringResource(id = R.string.remove_cover)
                is EditAction.ImageReplace -> stringResource(id = R.string.update_image)
            }
        )
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