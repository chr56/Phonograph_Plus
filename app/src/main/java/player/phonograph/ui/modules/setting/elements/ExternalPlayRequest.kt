/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.elements

import player.phonograph.R
import player.phonograph.model.SongClickMode
import player.phonograph.ui.compose.components.CheckBoxItem
import player.phonograph.ui.compose.components.ModeRadioBox
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ColumnScope.ExternalPlayRequestSettings(
    showPrompt: Boolean,
    flipUseDefault: () -> Unit,
    silence: Boolean,
    flipSilence: () -> Unit,
    currentModeSingle: Int,
    setCurrentModeSingle: (Int) -> Unit,
    currentModeMultiple: Int,
    setCurrentModeMultiple: (Int) -> Unit,
) {
    val context = LocalContext.current

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