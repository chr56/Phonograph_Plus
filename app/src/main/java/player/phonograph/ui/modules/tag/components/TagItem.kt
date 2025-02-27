/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.components

import player.phonograph.R
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.ui.compose.components.VerticalTextItem
import player.phonograph.ui.modules.tag.MetadataUIEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun EditableTagItem(
    key: ConventionalMusicMetadataKey,
    tagName: String,
    value: String,
    alternatives: Collection<String> = emptyList(),
    onEdit: (MetadataUIEvent.Edit) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // tag name
        Text(
            text = tagName,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
            modifier = Modifier
                .align(Alignment.Start),
        )
        // content
        val originalValue = remember(key) { value }
        var currentValue by remember(key) { mutableStateOf(value) }
        var hasEdited by remember(key) { mutableStateOf(false) }


        var indicatorColor by remember(key, originalValue) {
            mutableStateOf(Color(0xFF707070))
        }


        fun updateValue(newValue: String) {
            currentValue = newValue
            hasEdited = newValue != originalValue
            indicatorColor = if (hasEdited) Color(0xFFF59C01) else Color(0xFF707070)
        }

        fun submit() {
            onEdit.invoke(
                MetadataUIEvent.Edit.UpdateTag(key, currentValue)
            )
            hasEdited = false
            indicatorColor = Color(0xFF00C72C)
        }

        var showDropdownMenu by remember { mutableStateOf(false) }

        TextField(
            value = currentValue,
            onValueChange = ::updateValue,
            modifier = Modifier
                .align(Alignment.Start)
                .fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.background,
                textColor = MaterialTheme.colors.onSurface,
                focusedIndicatorColor = indicatorColor.copy(alpha = TextFieldDefaults.IconOpacity),
                unfocusedIndicatorColor = Color.Transparent,
            ),
            textStyle = TextStyle(fontSize = 14.sp),
            trailingIcon = {
                Row {
                    if (hasEdited) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(id = android.R.string.ok),
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable { submit() }
                        )
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(id = R.string.more_actions),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { showDropdownMenu = !showDropdownMenu }
                    )
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.delete_action),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                onEdit.invoke(MetadataUIEvent.Edit.RemoveTag(key))
                            }
                    )
                }
            }
        )
        DropdownMenu(
            expanded = showDropdownMenu,
            modifier = Modifier.fillMaxWidth(0.7f),
            onDismissRequest = {
                showDropdownMenu = false
            }
        ) {
            fun onClick(value: String) {
                showDropdownMenu = false
                updateValue(value)
            }
            for (alternative in alternatives) {
                DropdownMenuItem({}) {
                    Text(
                        text = alternative,
                        modifier =
                        Modifier.weight(5f)
                    )
                    Icon(
                        Icons.Default.Add, stringResource(R.string.add_action),
                        modifier = Modifier
                            .clickable { onClick("$currentValue ; $alternative") }
                            .weight(1f)
                    )
                    Icon(
                        Icons.Default.Check, stringResource(R.string.save),
                        modifier =
                        Modifier
                            .clickable { onClick(alternative) }
                            .weight(1f)
                    )

                }
            }
            DropdownMenuItem(onClick = { onClick(originalValue) }) {
                Text(text = " [${stringResource(id = R.string.reset_action)}] ")
            }
        }
    }

}

@Composable
fun ReadonlyTagItem(label: String, value: String) = VerticalTextItem(label, value)

@Composable
fun ReadonlyTagItem(label: String, value: List<String>) = VerticalTextItem(label, value.joinToString(",\n"))