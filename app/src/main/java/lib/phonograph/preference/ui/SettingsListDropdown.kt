/*
 * Copyright (c) 2021-2023 alorma
 */

package lib.phonograph.preference.ui

import lib.phonograph.preference.SettingValueState
import lib.phonograph.preference.rememberIntSettingState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun SettingsListDropdown(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state: SettingValueState<Int> = rememberIntSettingState(),
    title: @Composable () -> Unit,
    items: List<String>,
    icon: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    onItemSelected: ((Int, String) -> Unit)? = null,
    menuItem: (@Composable (index: Int, text: String) -> Unit)? = null,
) {
    if (state.value > items.size) {
        throw IndexOutOfBoundsException("Current value of state for list setting cannot be grater than items size")
    }
    val scrollState = rememberScrollState()

    Surface {
        InabilityColor(enabled = enabled) {
            var isDropdownExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = modifier.fillMaxWidth()
                    .clickable(enabled = enabled) { isDropdownExpanded = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.padding(end = 4.dp).weight(5f).widthIn(min=24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsTileIcon(icon = icon)
                    SettingsTileTexts(
                        title = title,
                        subtitle = subtitle,
                    )
                }

                Column(
                    modifier = Modifier.padding(end = 8.dp).widthIn(min=8.dp, max = 128.dp)
                        .verticalScroll(scrollState),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = items[state.value])
                        Icon(
                            modifier = Modifier.padding(start = 8.dp),
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                        )
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                    ) {
                        items.forEachIndexed { index, text ->
                            DropdownMenuItem(
                                onClick = {
                                    state.value = index
                                    isDropdownExpanded = false
                                    onItemSelected?.invoke(index, text)
                                },
                            ) {
                                if (menuItem != null) {
                                    menuItem(index, text)
                                } else {
                                    Text(text = text)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
