/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.components.ActionItem
import player.phonograph.ui.modules.setting.elements.CheckUpdateIntervalSettings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class CheckUpdateIntervalDialog : AbsSettingsDialog() {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val key = remember { Keys.checkUpdateInterval }
        val preference = remember { Setting(context)[key] }
        var duration by remember { mutableStateOf(preference.data) }
        SettingsDialog(
            modifier = Modifier,
            title = stringResource(R.string.pref_title_check_for_updates_interval),
            actions = listOf(
                ActionItem(
                    Icons.Default.Refresh,
                    textRes = R.string.action_reset,
                    onClick = {
                        preference.data = key.valueProvider.defaultValue()
                        dismiss()
                    }
                ),
                ActionItem(
                    Icons.Default.Check,
                    textRes = android.R.string.ok,
                    onClick = {
                        preference.data = duration
                        dismiss()
                    }
                ),
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                CheckUpdateIntervalSettings(
                    currentSelectedDuration = duration,
                    onChangeDuration = { duration = it },
                    previewTextTemplate = R.string.tips_preview_next_updates_check
                )
            }
        }
    }
}

