/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.components

import player.phonograph.util.testBit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ModeRadioBox(
    mode: Int,
    name: String,
    currentMode: MutableState<Int>,
    setCurrentMode: (Int) -> Unit,
) {
    Row(Modifier
        .clickable { setCurrentMode(mode) }
        .fillMaxWidth()) {
        RadioButton(selected = currentMode.value == mode, onClick = { setCurrentMode(mode) })
        Text(
            text = name,
            Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
                .alignByBaseline()
        )
    }
}

@Composable
fun FlagCheckBox(
    mask: Int,
    name: String,
    currentExtraFlag: MutableState<Int>,
    flipExtraFlagBit: (Int) -> Unit,
) {

    Row(Modifier
        .clickable { flipExtraFlagBit(mask) }
        .fillMaxWidth()
    ) {
        Checkbox(
            checked = currentExtraFlag.value.testBit(mask),
            onCheckedChange = { flipExtraFlagBit(mask) }
        )
        Text(
            text = name,
            Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
                .alignByBaseline()
        )
    }

}