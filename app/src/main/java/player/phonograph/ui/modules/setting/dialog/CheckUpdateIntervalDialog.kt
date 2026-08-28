/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.model.menu.ActionEntry
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import player.phonograph.ui.modules.setting.elements.CheckUpdateIntervalSettings
import player.phonograph.ui.resource.ActionSemantics
import androidx.compose.foundation.layout.padding
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
        val preference = remember { Settings(context)[key] }
        var duration by remember { mutableStateOf(preference.data) }
        SettingsDialog(
            modifier = Modifier,
            title = stringResource(R.string.pref_title_check_for_updates_interval),
            actions = listOf(
                ActionEntry.ToolbarEntry(
                    semantic = ActionSemantics.Reset,
                    action = {
                        preference.data = key.valueProvider.defaultValue()
                        dismiss()
                    }
                ),
                ActionEntry.ToolbarEntry(
                    semantic = ActionSemantics.Confirm,
                    action = {
                        preference.data = duration
                        dismiss()
                    }
                ),
            )
        ) {
            CheckUpdateIntervalSettings(
                currentSelectedDuration = duration,
                onChangeDuration = { duration = it },
                previewTextTemplate = R.string.tips_preview_next_updates_check,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

