/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import player.phonograph.R
import player.phonograph.model.metadata.EditAction
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp



class MetadataChanges(
    /**
     * <EditAction, oldValue> pair
     */
    val tagDiff: List<Pair<EditAction, String?>>,
) {
    fun noChange() = tagDiff.isEmpty()
}


@Composable
fun MetadataDifferenceScreen(diff: MetadataChanges) {
    if (diff.noChange()) {
        Text(text = stringResource(id = R.string.no_changes))
    } else {
        LazyColumn(Modifier.padding(8.dp)) {
            for (tag in diff.tagDiff) {
                item {
                    MetadataDifferenceItem(tag.first, tag.second)
                }
            }
        }
    }
}

@Composable
private fun MetadataDifferenceItem(action: EditAction, old: String?) {
    Column(Modifier.padding(vertical = 16.dp)) {
        val text = if (action.key.res > 0) stringResource(action.key.res) else action.key.name
        Title(text, horizontalPadding = 0.dp)
        NullableText(old)
        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        NullableText(
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
private fun NullableText(string: String?, modifier: Modifier = Modifier) {
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