/*
 * Copyright (c) 2021-2023 alorma
 */

package lib.phonograph.preference.ui

import com.alorma.compose.settings.storage.base.SettingValueState
import com.alorma.compose.settings.storage.base.getValue
import com.alorma.compose.settings.storage.base.rememberBooleanSettingState
import com.alorma.compose.settings.storage.base.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchColors
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp


@Composable
fun SettingsSwitch(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state: SettingValueState<Boolean> = rememberBooleanSettingState(),
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    switchColors: SwitchColors = SwitchDefaults.colors(),
    onCheckedChange: (Boolean) -> Unit = {},
) {
    var storageValue by state
    val update: (Boolean) -> Unit = { boolean ->
        storageValue = boolean
        onCheckedChange(storageValue)
    }
    Surface {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .toggleable(
                    enabled = enabled,
                    value = storageValue,
                    role = Role.Switch,
                    onValueChange = { update(!storageValue) },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WrapContentColor(enabled = enabled) {
                SettingsTileIcon(icon = icon)
                SettingsTileTexts(modifier = Modifier.weight(1f), title = title, subtitle = subtitle)
                SettingsTileAction {
                    Switch(
                        checked = storageValue,
                        onCheckedChange = update,
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = enabled,
                        colors = switchColors,
                    )
                }
            }
        }
    }
}