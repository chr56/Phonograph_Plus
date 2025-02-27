/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.components

import player.phonograph.R
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.ui.modules.tag.MetadataUIEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context

/**
 * @param keys keys to display
 */
@Composable
fun InsertNewButton(
    keys: Set<ConventionalMusicMetadataKey>,
    onEdit: (Context, MetadataUIEvent.Edit) -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        var showed by remember { mutableStateOf(false) }
        DropdownMenu(expanded = showed, onDismissRequest = { showed = false }) {
            val context = LocalContext.current
            for (fieldKey in keys) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showed = false
                        onEdit(context, MetadataUIEvent.Edit.AddNewTag(fieldKey))
                    }
                    .padding(8.dp, 16.dp)
                ) {
                    Text(
                        text = if (fieldKey.res > 0) stringResource(fieldKey.res) else fieldKey.name,
                        // modifier = Modifier.align(Alignment.Start),
                    )
                    Text(
                        text = fieldKey.name,
                        // modifier = Modifier.align(Alignment.End),
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.77f),
                            fontSize = 8.sp,
                        )
                    )
                }
            }
        }
        TextButton(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .padding(0.dp, 16.dp),
            onClick = {
                showed = true
            }
        ) {
            Row(
                Modifier.fillMaxWidth()
            )
            {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_white_24dp),
                    contentDescription = stringResource(id = R.string.add_action),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterVertically),
                    tint = MaterialTheme.colors.onSurface
                )
                Text(
                    text = stringResource(id = R.string.add_action),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .fillMaxWidth(),
                    color = MaterialTheme.colors.onSurface
                )
            }
        }
    }
}