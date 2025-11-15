/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.components.ActionItem
import player.phonograph.ui.modules.setting.elements.ExternalPlayRequestSettings
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class ExternalPlayRequestSettingDialog : AbsSettingsDialog() {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        var showPrompt by remember {
            mutableStateOf(Setting(context)[Keys.externalPlayRequestShowPrompt].data)
        }
        val flipUseDefault = {
            val newValue = !showPrompt
            showPrompt = newValue
            Setting(context)[Keys.externalPlayRequestShowPrompt].data = newValue
        }

        var silence by remember {
            mutableStateOf(Setting(context)[Keys.externalPlayRequestSilence].data)
        }
        val flipSilence = {
            val newValue = !silence
            silence = newValue
            Setting(context)[Keys.externalPlayRequestSilence].data = newValue
        }

        var currentModeSingle by remember {
            mutableIntStateOf(Setting(context)[Keys.externalPlayRequestSingleMode].data)
        }
        val setCurrentModeSingle = { new: Int ->
            currentModeSingle = new
            Setting(context)[Keys.externalPlayRequestSingleMode].data = new
        }

        var currentModeMultiple by remember {
            mutableIntStateOf(Setting(context)[Keys.externalPlayRequestMultipleMode].data)
        }
        val setCurrentModeMultiple = { new: Int ->
            currentModeMultiple = new
            Setting(context)[Keys.externalPlayRequestMultipleMode].data = new
        }
        SettingsDialog(
            modifier = Modifier,
            title = stringResource(R.string.pref_title_external_play_request),
            actions = listOf(
                ActionItem(
                    Icons.Default.Check,
                    textRes = android.R.string.ok,
                    onClick = { dismiss() }
                )
            ),
        ) {
            Surface(
                modifier = Modifier
                    .heightIn(min = 120.dp, max = 480.dp)
                    .padding(vertical = 16.dp),
                elevation = 8.dp
            ) {
                Column(
                    Modifier
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    ExternalPlayRequestSettings(
                        showPrompt = showPrompt,
                        flipUseDefault = flipUseDefault,
                        silence = silence,
                        flipSilence = flipSilence,
                        currentModeSingle = currentModeSingle,
                        setCurrentModeSingle = setCurrentModeSingle,
                        currentModeMultiple = currentModeMultiple,
                        setCurrentModeMultiple = setCurrentModeMultiple,
                    )
                }
            }
        }
    }
}

