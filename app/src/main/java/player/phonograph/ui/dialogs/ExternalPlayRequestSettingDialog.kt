/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.model.SongClickMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.CheckBoxItem
import player.phonograph.ui.compose.components.ModeRadioBox
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Context

class ExternalPlayRequestSettingDialog : ComposeViewDialogFragment() {
    @Composable
    override fun Content() {
        val dialogState = rememberMaterialDialogState(true)
        PhonographTheme {
            MaterialDialog(
                dialogState = dialogState,
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
                ExternalPlayRequestSettingDialogContent(requireContext())
            }
        }
    }
}

@Composable
private fun ExternalPlayRequestSettingDialogContent(context: Context) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
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

        CheckBoxItem(stringResource(R.string.pref_option_show_prompt), showPrompt, flip = flipUseDefault)

        CheckBoxItem(stringResource(R.string.pref_option_silence), silence, flip = flipSilence)

        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(R.string.single_item), style = MaterialTheme.typography.h6)
        for (id in SongClickMode.singleItemModes) {
            ModeRadioBox(
                mode = id,
                name = SongClickMode.modeName(context.resources, id),
                selectedMode = currentModeSingle,
                setCurrentMode = setCurrentModeSingle,
                enabled = !showPrompt,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(R.string.multiple_items), style = MaterialTheme.typography.h6)
        for (id in SongClickMode.multipleItemsModes) {
            ModeRadioBox(
                mode = id,
                name = SongClickMode.modeName(context.resources, id),
                selectedMode = currentModeMultiple,
                setCurrentMode = setCurrentModeMultiple,
                enabled = !showPrompt,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}