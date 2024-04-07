/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.model.SongClickMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.DialogContent
import player.phonograph.ui.compose.components.FlagCheckBox
import player.phonograph.ui.compose.components.ModeRadioBox
import player.phonograph.util.setBit
import player.phonograph.util.testBit
import player.phonograph.util.unsetBit
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Context

class ClickModeSettingDialog : ComposeViewDialogFragment() {
    @Composable
    override fun Content() {
        val dialogState = rememberMaterialDialogState(true)
        PhonographTheme {
            MaterialDialog(
                dialogState = dialogState,
                elevation = 0.dp,
                onCloseRequest = { dismiss() },
                buttons = {
                    button(
                        res = android.R.string.ok,
                        textStyle = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
                    ) {
                        dismiss()
                    }
                }
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                title(res = R.string.pref_title_click_behavior)
                customView {
                    DialogContent {
                        ClickModeSettingDialogContent(requireContext())
                    }
                }
            }
        }
    }
}

@Composable
private fun ClickModeSettingDialogContent(context: Context) {
    Column {
        val currentMode = remember {
            mutableStateOf(Setting(context)[Keys.songItemClickMode].data)
        }
        val setCurrentMode = { new: Int ->
            currentMode.value = new
            Setting(context)[Keys.songItemClickMode].data = new
        }
        for (id in SongClickMode.baseModes) {
            ModeRadioBox(
                mode = id,
                name = SongClickMode.modeName(context.resources, id),
                currentMode = currentMode,
                setCurrentMode = setCurrentMode
            )
        }

        Spacer(Modifier.height(8.dp))

        val currentExtraFlag = remember {
            mutableStateOf(Setting(context)[Keys.songItemClickExtraFlag].data)
        }
        val flipExtraFlagBit = { mask: Int ->
            val new = if (currentExtraFlag.value.testBit(mask)) {
                currentExtraFlag.value.unsetBit(mask)
            } else {
                currentExtraFlag.value.setBit(mask)
            }
            currentExtraFlag.value = new
            Setting(context)[Keys.songItemClickExtraFlag].data = new
        }

        FlagCheckBox(
            mask = SongClickMode.FLAG_MASK_GOTO_POSITION_FIRST,
            name = context.getString(R.string.mode_flag_goto_position_first),
            currentExtraFlag = currentExtraFlag,
            flipExtraFlagBit = flipExtraFlagBit
        )
        FlagCheckBox(
            mask = SongClickMode.FLAG_MASK_PLAY_QUEUE_IF_EMPTY,
            name = context.getString(R.string.mode_flag_play_queue_if_empty),
            currentExtraFlag = currentExtraFlag,
            flipExtraFlagBit = flipExtraFlagBit
        )
    }
}