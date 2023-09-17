/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.dialogs

import player.phonograph.model.time.CalculationMode
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeUnit
import player.phonograph.model.time.displayText
import player.phonograph.ui.compose.components.WheelPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PastTimeIntervalPicker(
    selectedMode: CalculationMode,
    selected: Duration,
    onChangeMode: (CalculationMode) -> Unit,
    onChangeDuration: (Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    val modes = remember { CalculationMode.values() }

    val units = remember { TimeUnit.values() }
    val numbers = remember { 1..30 }

    var currentNumber by remember { mutableStateOf(selected.value) }
    var currentUnit by remember { mutableStateOf(selected.unit) }

    Row(
        modifier,
        Arrangement.SpaceBetween
    ) {
        WheelPicker(
            items = modes.map { it.displayText(resources) },
            initialIndex = modes.indexOf(selectedMode),
            modifier = Modifier
                .weight(5f)
                .padding(horizontal = 6.dp)
        ) {
            onChangeMode(modes[it])
        }

        WheelPicker(
            items = numbers.map { it.toString() },
            initialIndex = (selected.value + 1).coerceAtMost(30).toInt(),
            modifier = Modifier
                .weight(6f)
                .padding(horizontal = 6.dp)
        ) {
            currentNumber = (it + 1).toLong()
            onChangeDuration(Duration.of((it + 1).toLong(), currentUnit))
        }

        WheelPicker(
            items = units.map { it.displayText(resources) },
            initialIndex = units.indexOf(selected.unit),
            modifier = Modifier
                .weight(6f)
                .padding(horizontal = 6.dp)
        ) {
            currentUnit = units[it]
            onChangeDuration(Duration.of(currentNumber, units[it]))
        }
    }


}