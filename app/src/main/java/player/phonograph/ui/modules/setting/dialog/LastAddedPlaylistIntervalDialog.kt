/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.modules.setting.elements.LastAddedPlaylistIntervalSettings
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class LastAddedPlaylistIntervalDialog : ComposeViewDialogFragment() {
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

        PhonographTheme {
            MaterialDialog(
                dialogState = rememberMaterialDialogState(true),
                onCloseRequest = { dismiss() },
                buttons = {
                    negativeButton(
                        res = android.R.string.cancel,
                        textStyle = accentColoredButtonStyle()
                    ) { dismiss() }
                    positiveButton(
                        res = android.R.string.ok,
                        textStyle = accentColoredButtonStyle()
                    ) {
                        dismiss()
                        synchronized(this) {
                            Setting(context)[Keys._lastAddedCutOffMode].data = currentlySelectedMode.value
                            Setting(context)[Keys._lastAddedCutOffDuration].data = currentlySelected.serialise()
                        }
                    }
                }
            ) {
                title(res = R.string.pref_title_last_added_interval)
                Column(
                    Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    LastAddedPlaylistIntervalSettings(
                        currentSelectedMode = currentlySelectedMode,
                        onChangeMode = { calculationMode -> currentlySelectedMode = calculationMode },
                        currentSelectedDuration = currentlySelected,
                        onChangeDuration = { duration -> currentlySelected = duration },
                        previewTextTemplate = R.string.preview_text_cutoff_time_interval
                    )
                }
            }
        }
    }
}