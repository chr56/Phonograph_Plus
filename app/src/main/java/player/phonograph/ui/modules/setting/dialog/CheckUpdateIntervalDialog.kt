/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.time.Duration
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.modules.setting.elements.CheckUpdateIntervalSettings
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

class CheckUpdateIntervalDialog : ComposeViewDialogFragment() {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        var duration: Duration by remember {
            mutableStateOf(Setting(context)[Keys.checkUpdateInterval].data)
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
                            Setting(App.instance)[Keys.checkUpdateInterval].data = duration
                        }
                    }
                }
            ) {
                title(res = R.string.pref_title_check_for_updates_interval)
                Column(
                    Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    CheckUpdateIntervalSettings(
                        currentSelectedDuration = duration,
                        onChangeDuration = { duration = it },
                        previewTextTemplate = R.string.tips_preview_next_updates_check
                    )
                }
            }
        }
    }
}

