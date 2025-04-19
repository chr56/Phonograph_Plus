/*
 * Copyright (c) 2022 chr_56
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
import player.phonograph.ui.modules.setting.elements.ClickModeSettings
import player.phonograph.util.setBit
import player.phonograph.util.testBit
import player.phonograph.util.theme.accentColoredButtonStyle
import player.phonograph.util.unsetBit
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class ClickModeSettingDialog : ComposeViewDialogFragment() {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        var currentMode by remember {
            mutableIntStateOf(Setting(context)[Keys.songItemClickMode].data)
        }
        val setCurrentMode = { new: Int ->
            currentMode = new
            Setting(context)[Keys.songItemClickMode].data = new
        }
        var currentExtraFlag by remember {
            mutableIntStateOf(Setting(context)[Keys.songItemClickExtraFlag].data)
        }
        val flipExtraFlagBit = { mask: Int ->
            val new = if (currentExtraFlag.testBit(mask)) {
                currentExtraFlag.unsetBit(mask)
            } else {
                currentExtraFlag.setBit(mask)
            }
            currentExtraFlag = new
            Setting(context)[Keys.songItemClickExtraFlag].data = new
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
                title(res = R.string.pref_title_click_behavior)
                Column(
                    Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    ClickModeSettings(
                        currentMode = currentMode,
                        setCurrentMode = setCurrentMode,
                        currentExtraFlag = currentExtraFlag,
                        flipExtraFlagBit = flipExtraFlagBit,
                    )
                }
            }
        }
    }
}