/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.elements

import player.phonograph.R
import player.phonograph.model.SongClickMode
import player.phonograph.ui.compose.components.FlagCheckBox
import player.phonograph.ui.compose.components.ModeRadioBox
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ClickModeSettings(
    currentMode: Int,
    setCurrentMode: (Int) -> Unit,
    currentExtraFlag: Int,
    flipExtraFlagBit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth()
    ) {
        for (id in SongClickMode.allModes) {
            ModeRadioBox(
                mode = id,
                name = SongClickMode.modeName(LocalResources.current, id),
                selectedMode = currentMode,
                setCurrentMode = setCurrentMode
            )
        }
        Spacer(Modifier.height(8.dp))
        FlagCheckBox(
            mask = SongClickMode.FLAG_MASK_GOTO_POSITION_FIRST,
            name = stringResource(R.string.mode_flag_goto_position_first),
            currentFlag = currentExtraFlag,
            flipFlagBit = flipExtraFlagBit
        )
        FlagCheckBox(
            mask = SongClickMode.FLAG_MASK_PLAY_QUEUE_IF_EMPTY,
            name = stringResource(R.string.mode_flag_play_queue_if_empty),
            currentFlag = currentExtraFlag,
            flipFlagBit = flipExtraFlagBit
        )
        Spacer(Modifier.height(8.dp))

    }
}