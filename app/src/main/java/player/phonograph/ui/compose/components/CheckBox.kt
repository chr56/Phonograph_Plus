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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ModeRadioBox(
    name: String,
    mode: Int,
    selectedMode: Int,
    enabled: Boolean = true,
    setCurrentMode: (Int) -> Unit,
) {
    Row(Modifier
        .clickable { setCurrentMode(mode) }
        .fillMaxWidth()) {
        RadioButton(selected = selectedMode == mode, onClick = { setCurrentMode(mode) }, enabled = enabled)
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
    name: String,
    mask: Int,
    currentFlag: Int,
    flipFlagBit: (Int) -> Unit,
) {
    CheckBoxItem(name, checked = currentFlag.testBit(mask)) {
        flipFlagBit(mask)
    }
}


@Composable
fun CheckBoxItem(
    name: String,
    checked: Boolean,
    enabled: Boolean = true,
    flip: () -> Unit,
) {
    Row(Modifier
        .clickable { flip() }
        .fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { flip() },
            enabled = enabled
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