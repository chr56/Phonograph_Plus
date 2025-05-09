/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.elements

import player.phonograph.R
import player.phonograph.model.ui.NowPlayingScreenStyle
import player.phonograph.model.ui.PlayerBaseStyle
import player.phonograph.model.ui.PlayerControllerStyle
import player.phonograph.model.ui.PlayerOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp


@Composable
fun ColumnScope.NowPlayingScreenStyleSettings(
    current: NowPlayingScreenStyle,
    update: (NowPlayingScreenStyle) -> Unit,
) {
    val playerBaseStyle = current.baseStyle
    val controllerStyle = current.controllerStyle
    val options = current.options
    PlayerBaseStyle(playerBaseStyle) { update(current.copy(baseStyle = it)) }
    PlayerControllerStyle(controllerStyle) { update(current.copy(controllerStyle = it)) }
    PlayerControllerButtonFunctions(controllerStyle) { update(current.copy(controllerStyle = it)) }
    PlayerStyleOptions(options) { update(current.copy(options = it)) }
}


@Composable
private fun PlayerBaseStyle(
    current: PlayerBaseStyle,
    update: (PlayerBaseStyle) -> Unit,
) {
    val selected = remember(current) { current.ordinal }
    val styles = remember { PlayerBaseStyle.entries }
    val names = styles.map {
        when (it) {
            PlayerBaseStyle.CARD -> stringResource(R.string.style_card)
            PlayerBaseStyle.FLAT -> stringResource(R.string.style_flat)
        }
    }

    ListOption(
        current = selected,
        title = stringResource(R.string.pref_title_now_playing_screen_style),
        description = "",
        options = names,
        onOptionSelected = { index, _ -> update(styles[index]) }
    )
}

private val AllControllerStyles = listOf(
    PlayerControllerStyle.STYLE_CLASSIC,
    PlayerControllerStyle.STYLE_FLAT,
)

@Composable
private fun PlayerControllerStyle(
    current: PlayerControllerStyle,
    update: (PlayerControllerStyle) -> Unit,
) {
    val stylesNames = listOf(
        stringResource(R.string.player_controller_style_classic),
        stringResource(R.string.player_controller_style_flat),
    )
    val selectedStyle = AllControllerStyles.indexOf(current.style).coerceIn(0, AllControllerStyles.size)
    ListOption(
        current = selectedStyle,
        title = stringResource(R.string.pref_title_player_controller_style),
        description = "",
        options = stylesNames,
        onOptionSelected = { index, _ -> update(current.copy(style = AllControllerStyles[index])) }
    )
}

private val AllButtons = listOf(
    PlayerControllerStyle.BUTTONS_PRIMARY,
    PlayerControllerStyle.BUTTONS_SECONDARY,
    PlayerControllerStyle.BUTTONS_TERTIARY,
)

private val AllButtonFunctions = listOf(
    PlayerControllerStyle.FUNCTION_NONE,
    PlayerControllerStyle.FUNCTION_SWITCH,
    PlayerControllerStyle.FUNCTION_SEEK,
    PlayerControllerStyle.FUNCTION_QUEUE_MODE_N,
    PlayerControllerStyle.FUNCTION_QUEUE_MODE_A
)

@Composable
private fun PlayerControllerButtonFunctions(
    current: PlayerControllerStyle,
    update: (PlayerControllerStyle) -> Unit,
) {
    val buttonNames = listOf(
        stringResource(R.string.player_controller_buttons_primary),
        stringResource(R.string.player_controller_buttons_secondary),
        stringResource(R.string.player_controller_buttons_tertiary),
    )
    val buttonDescriptions = listOf(
        stringResource(R.string.player_controller_buttons_primary_description),
        stringResource(R.string.player_controller_buttons_secondary_description),
        stringResource(R.string.player_controller_buttons_tertiary_description),
    )
    val functionsNames = listOf(
        stringResource(R.string.player_controller_function_none),
        stringResource(R.string.player_controller_function_switch),
        stringResource(R.string.player_controller_function_seek),
        stringResource(R.string.player_controller_function_queue_mode_normal),
        stringResource(R.string.player_controller_function_queue_mode_alternative),
    )
    for ((buttonOrder, buttonId) in AllButtons.withIndex()) {
        val function = current.buttons.getOrDefault(buttonId, -1)
        val selectedFunction = AllButtonFunctions.indexOf(function).coerceIn(0, AllButtonFunctions.size)
        val title = stringResource(R.string.player_controller_designate_button_functions, buttonNames[buttonOrder])
        val description = buttonDescriptions[buttonOrder]
        ListOption(
            current = selectedFunction,
            title = title,
            description = description,
            options = functionsNames,
            onOptionSelected = { index, _ ->
                val buttonsMap = current.buttons.toMutableMap().also {
                    it[buttonId] = AllButtonFunctions[index]
                }
                update(current.copy(buttons = buttonsMap))
            }
        )
    }
}


@Composable
private fun PlayerStyleOptions(
    current: PlayerOptions,
    update: (PlayerOptions) -> Unit,
) {
    CheckBoxOption(stringResource(R.string.pref_title_display_modes_with_queue), current.showModeButtonsForQueue) {
        update(current.copy(showModeButtonsForQueue = it))
    }
}

@Composable
private fun ListOption(
    current: Int,
    title: String,
    description: String,
    options: List<String>,
    onOptionSelected: (Int, String) -> Unit,
) {
    Surface(
        Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        var isDropdownExpanded by remember { mutableStateOf(false) }
        val selected = remember { mutableIntStateOf(current) }

        Column(
            modifier = Modifier.clickable { isDropdownExpanded = true },
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Title
            Text(
                title,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Description
            Text(
                description,
                style = MaterialTheme.typography.caption, color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )
            // Options
            Row(
                modifier = Modifier
                    .widthIn(max = 160.dp)
                    .padding(vertical = 4.dp)
                    .align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = options[selected.intValue],
                    modifier = Modifier.weight(5f),
                )
                Icon(
                    Icons.Outlined.ArrowDropDown, contentDescription = null,
                    modifier = Modifier.weight(1f),
                )

                DropdownMenu(
                    modifier = Modifier,
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                ) {
                    options.forEachIndexed { index, text ->
                        DropdownMenuItem(
                            onClick = {
                                selected.intValue = index
                                isDropdownExpanded = false
                                onOptionSelected(index, text)
                            },
                        ) {
                            Text(text = text)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckBoxOption(
    title: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    Row(Modifier
        .clickable { onChanged(!checked) }
        .heightIn(64.dp, 96.dp)
        .padding(horizontal = 8.dp)
    ) {
        Text(
            text = title,
            Modifier
                .weight(4f)
                .align(Alignment.CenterVertically)
                .alignByBaseline()
        )
        Checkbox(
            checked = checked,
            onCheckedChange = { onChanged(it) },
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        )
    }
}
