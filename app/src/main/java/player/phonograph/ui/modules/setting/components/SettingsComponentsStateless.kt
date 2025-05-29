/*
 *  Copyright (c) 2021-2023 alorma
 */

package player.phonograph.ui.modules.setting.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Slider
import androidx.compose.material.SliderColors
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchColors
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt

//region Stateless Settings Components
@Composable
fun SettingsGroup(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface {
        Column(
            modifier = modifier
                .fillMaxWidth(),
        ) {
            // Title
            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 16.dp)
                    .height(64.dp),
                contentAlignment = Alignment.Companion.CenterStart,
            ) {
                val titleStyle = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.primary)
                ProvideTextStyle(value = titleStyle) { title() }
            }
            content()
        }
    }
}

@Composable
fun SettingsSwitch(
    value: Boolean,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    switchColors: SwitchColors = SwitchDefaults.colors(),
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Surface {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .toggleable(
                    enabled = enabled,
                    value = value,
                    role = Role.Companion.Switch,
                    onValueChange = { onCheckedChange(!value) },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InabilityColor(enabled = enabled) {
                SettingsTileIcon(icon = icon)
                SettingsTileTexts(modifier = Modifier.weight(1f), title = title, subtitle = subtitle)
                SettingsTileAction {
                    Switch(
                        checked = value,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = enabled,
                        colors = switchColors,
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsListDropdown(
    selected: Int,
    title: @Composable () -> Unit,
    options: List<String>,
    onOptionItemSelected: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    optionsItem: (@Composable (index: Int, text: String) -> Unit)? = null,
) {
    if (selected !in options.indices) {
        throw IndexOutOfBoundsException("Current value of state for list setting cannot be grater than items size")
    }
    val scrollState = rememberScrollState()

    Surface {
        InabilityColor(enabled = enabled) {
            var isDropdownExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { isDropdownExpanded = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .weight(5f)
                        .widthIn(min = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SettingsTileIcon(icon = icon)
                    SettingsTileTexts(
                        title = title,
                        subtitle = subtitle,
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .widthIn(min = 8.dp, max = 128.dp)
                        .verticalScroll(scrollState),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = options[selected])
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
                        options.forEachIndexed { index, text ->
                            DropdownMenuItem(
                                onClick = {
                                    isDropdownExpanded = false
                                    onOptionItemSelected(index, text)
                                },
                            ) {
                                if (optionsItem != null) {
                                    optionsItem(index, text)
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

@Composable
fun SettingsSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    steps: Int = 0,
    onValueChange: (Float) -> Unit = {},
    onValueChangeFinished: () -> Unit = {},
) {
    Surface {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InabilityColor(enabled = enabled) {
                SettingsTileIcon(icon = icon)
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp)
                ) {
                    SettingsTileTexts(title = title, subtitle = subtitle)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Slider(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .weight(9f)
                                .padding(horizontal = 8.dp),
                            valueRange = valueRange,
                            steps = steps,
                            onValueChangeFinished = onValueChangeFinished,
                            enabled = enabled,
                            colors = colors,
                        )
                        Text(
                            text = value.fastRoundToInt().toString(),
                            modifier = Modifier
                                .weight(2f)
                                .padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsMenuLink(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    action: (@Composable (Boolean) -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    Surface {
        val contentModifier = Modifier.clickable(enabled = enabled, onClick = onClick)
        SettingsCustomizable(
            title = title,
            modifier = modifier,
            contentModifier = contentModifier,
            enabled = enabled,
            icon = icon,
            subtitle = subtitle,
        ) {
            if (action != null) {
                Divider(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .height(56.dp)
                        .width(1.dp),
                )
                SettingsTileAction {
                    action(enabled)
                }
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsExternal(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    Surface {
        val contentModifier = Modifier.combinedClickable(
            enabled = enabled, onClick = onClick, onLongClick = onLongClick
        )
        SettingsCustomizable(
            title = title,
            modifier = modifier,
            contentModifier = contentModifier,
            enabled = enabled,
            icon = icon,
            subtitle = subtitle,
        ) {
            Spacer(modifier = Modifier.width(16.dp))
        }
    }

}

@Composable
private fun SettingsCustomizable(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    actionTile: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        InabilityColor(enabled = enabled) {
            Row(
                modifier = contentModifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsTileIcon(icon = icon)
                SettingsTileTexts(title = title, subtitle = subtitle)
            }
            actionTile()
        }
    }
}
//endregion


//region Elemental Components

@Composable
fun SettingsTileTexts(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        SettingsTileTitle(title)
        if (subtitle != null) {
            Spacer(modifier = Modifier.size(2.dp))
            SettingsTileSubtitle(subtitle)
        }
    }
}


@Composable
private fun SettingsTileTitle(title: @Composable () -> Unit) {
    ProvideTextStyle(value = MaterialTheme.typography.subtitle1) {
        title()
    }
}

@Composable
private fun SettingsTileSubtitle(subtitle: @Composable () -> Unit) {
    ProvideTextStyle(value = MaterialTheme.typography.caption) {
        CompositionLocalProvider(
            LocalContentAlpha provides ContentAlpha.medium,
            content = subtitle,
        )
    }
}


@Composable
fun SettingsTileAction(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun SettingsTileIcon(
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    if (icon == null) {
        Spacer(
            modifier = modifier
                .padding(end = 16.dp)
                .size(width = 16.dp, height = 64.dp),
        )
    } else {
        Box(
            modifier = modifier.size(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

@Composable
fun InabilityColor(
    enabled: Boolean,
    alphaDisabled: Float = 0.4f,
    alphaEnabled: Float = 1.0f,
    content: @Composable () -> Unit,
) {
    val alpha = if (enabled) alphaEnabled else alphaDisabled
    val contentColor = LocalContentColor.current.copy(alpha = alpha)
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalContentAlpha provides alpha,
    ) {
        content()
    }
}

//endregion