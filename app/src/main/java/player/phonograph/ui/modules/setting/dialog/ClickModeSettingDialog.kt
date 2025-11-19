/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.components.ActionItem
import player.phonograph.ui.modules.setting.elements.ClickModeSettings
import player.phonograph.util.setBit
import player.phonograph.util.testBit
import player.phonograph.util.unsetBit
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class ClickModeSettingDialog : AbsSettingsDialog() {
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
        SettingsDialog(
            modifier = Modifier,
            title = stringResource(R.string.pref_title_click_behavior),
            actions = listOf(
                ActionItem(
                    Icons.Default.Check,
                    textRes = android.R.string.ok,
                    onClick = { dismiss() }
                )
            ),
            scrollable = true,
            innerShadow = true,
        ) {
            ClickModeSettings(
                currentMode = currentMode,
                setCurrentMode = setCurrentMode,
                currentExtraFlag = currentExtraFlag,
                flipExtraFlagBit = flipExtraFlagBit,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}