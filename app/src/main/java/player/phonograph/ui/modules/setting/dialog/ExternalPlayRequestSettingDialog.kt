/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.modules.setting.elements.ExternalPlayRequestSettings
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class ExternalPlayRequestSettingDialog : ComposeViewDialogFragment() {
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
        PhonographTheme {
            MaterialDialog(
                dialogState = rememberMaterialDialogState(true),
                elevation = 0.dp,
                onCloseRequest = { dismiss() },
                buttons = {
                    positiveButton(
                        res = android.R.string.ok,
                        textStyle = accentColoredButtonStyle()
                    ) {
                        dismiss()
                    }
                }
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                title(res = R.string.pref_title_external_play_request)
                Column(
                    Modifier
                        .padding(24.dp)
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

