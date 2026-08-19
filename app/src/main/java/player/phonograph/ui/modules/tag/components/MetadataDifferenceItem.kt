/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.components

import player.phonograph.R
import player.phonograph.model.metadata.EditAction
import player.phonograph.ui.compose.components.display.Title
import player.phonograph.ui.resource.Texts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp


@Composable
fun MetadataDifferenceItem(action: EditAction, old: String?) {
    Column(Modifier.padding(vertical = 16.dp)) {
        val resources = LocalResources.current
        val text = remember(action.key) { Texts.metadataTagKey(resources, action.key) }
        Title(text, horizontalPadding = 0.dp)
        NullableText(old)
        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        NullableText(
            when (action) {
                is EditAction.Update       -> action.newValue
                is EditAction.Delete       -> stringResource(id = R.string.msg_empty)
                EditAction.ImageDelete     -> stringResource(id = R.string.action_remove_cover)
                is EditAction.ImageReplace -> stringResource(id = R.string.action_update_image)
            }
        )
    }
}

@Composable
private fun NullableText(string: String?, modifier: Modifier = Modifier) {
    if (string.isNullOrEmpty()) {
        Text(
            stringResource(id = R.string.msg_empty),
            modifier
                .fillMaxWidth()
                .alpha(0.5f)
        )
    } else {
        Text(string, modifier.fillMaxWidth())
    }
}