/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.components.ActionItem
import player.phonograph.ui.modules.setting.elements.LastAddedPlaylistIntervalSettings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class LastAddedPlaylistIntervalDialog : AbsSettingsDialog() {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        var currentlySelectedMode: TimeIntervalCalculationMode by remember {
            val preference = Setting(context)[Keys._lastAddedCutOffMode]
            val mode = (TimeIntervalCalculationMode.from(preference.data)
                ?: TimeIntervalCalculationMode.from(Keys._lastAddedCutOffMode.defaultValue()))!!
            mutableStateOf(mode)
        }
        var currentlySelected: Duration by remember {
            val preference = Setting(context)[Keys._lastAddedCutOffDuration]
            val duration =
                Duration.from(preference.data) ?: Duration.from(Keys._lastAddedCutOffDuration.defaultValue())!!
            mutableStateOf(duration)
        }
        SettingsDialog(
            modifier = Modifier,
            title = stringResource(R.string.pref_title_last_added_interval),
            actions = listOf(
                ActionItem(
                    Icons.Default.Check,
                    textRes = android.R.string.ok,
                    onClick = {
                        Setting(context)[Keys._lastAddedCutOffMode].data = currentlySelectedMode.value
                        Setting(context)[Keys._lastAddedCutOffDuration].data = currentlySelected.serialise()
                    }
                )
            ),
        ) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                LastAddedPlaylistIntervalSettings(
                    currentSelectedMode = currentlySelectedMode,
                    onChangeMode = { calculationMode -> currentlySelectedMode = calculationMode },
                    currentSelectedDuration = currentlySelected,
                    onChangeDuration = { duration -> currentlySelected = duration },
                    previewTextTemplate = R.string.tips_preview_cutoff_time_interval
                )
            }
        }
    }
}