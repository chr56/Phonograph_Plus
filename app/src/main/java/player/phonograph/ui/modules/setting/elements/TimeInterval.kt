/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.elements

import player.phonograph.R
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.model.time.TimeUnit
import player.phonograph.model.time.displayText
import player.phonograph.ui.compose.components.WheelPicker
import player.phonograph.util.time.TimeInterval
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import android.content.res.Resources
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LastAddedPlaylistIntervalSettings(
    currentSelectedMode: TimeIntervalCalculationMode,
    onChangeMode: (TimeIntervalCalculationMode) -> Unit,
    currentSelectedDuration: Duration,
    onChangeDuration: (Duration) -> Unit,
    @StringRes previewTextTemplate: Int,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    var preview by remember { mutableStateOf("") }

    var version by remember { mutableIntStateOf(0) }
    LaunchedEffect(version) {
        preview = previewText(
            resources = resources,
            text = previewTextTemplate,
            calculationMode = currentSelectedMode,
            duration = currentSelectedDuration
        )
    }

    val supportedDurationTimeUnits = remember { TimeUnit.entries }
    val supportedDurationValue = remember { (1..30).toList() }

    Column(modifier) {
        IntervalPicker(
            selectedDuration = currentSelectedDuration,
            supportedDurationTimeUnits = supportedDurationTimeUnits,
            supportedDurationValue = supportedDurationValue,
            onChangeDuration = { duration ->
                version++
                onChangeDuration(duration)
            },
            selectedMode = currentSelectedMode,
            supportedMode = remember { TimeIntervalCalculationMode.entries },
            onChangeMode = { calculationMode ->
                version++
                onChangeMode(calculationMode)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        PreviewDescription(preview)
    }
}

@Composable
fun CheckUpdateIntervalSettings(
    currentSelectedDuration: Duration,
    onChangeDuration: (Duration) -> Unit,
    @StringRes previewTextTemplate: Int,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    var preview by remember { mutableStateOf("") }

    var version by remember { mutableIntStateOf(0) }
    LaunchedEffect(version) {
        preview = previewText(
            resources = resources,
            text = previewTextTemplate,
            duration = currentSelectedDuration
        )
    }

    val supportedDurationTimeUnits = remember { listOf(TimeUnit.Week, TimeUnit.Day, TimeUnit.Hour) }
    val supportedDurationValue = remember { (1..24).toList() }
    Column(modifier) {
        IntervalPicker(
            selectedDuration = currentSelectedDuration,
            supportedDurationTimeUnits = supportedDurationTimeUnits,
            supportedDurationValue = supportedDurationValue,
            onChangeDuration = { duration ->
                version++
                onChangeDuration(duration)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        PreviewDescription(preview)
    }
}

@Composable
fun IntervalPicker(
    selectedDuration: Duration,
    supportedDurationTimeUnits: List<TimeUnit>,
    supportedDurationValue: List<Int>,
    onChangeDuration: (Duration) -> Unit,
    modifier: Modifier = Modifier,
    selectedMode: TimeIntervalCalculationMode? = null,
    supportedMode: List<TimeIntervalCalculationMode> = emptyList(),
    onChangeMode: ((TimeIntervalCalculationMode) -> Unit)? = null,
) {
    val resources = LocalResources.current

    var currentNumber by remember { mutableLongStateOf(selectedDuration.value) }
    var currentUnit by remember { mutableStateOf(selectedDuration.unit) }

    Row(
        modifier,
        Arrangement.SpaceBetween
    ) {
        if (selectedMode != null && supportedMode.isNotEmpty() && onChangeMode != null) {
            WheelPicker(
                items = supportedMode.map { it.displayText(resources) },
                initialIndex = supportedMode.indexOf(selectedMode),
                modifier = Modifier
                    .weight(5f)
                    .padding(horizontal = 6.dp)
            ) {
                onChangeMode(supportedMode[it])
            }
        }

        WheelPicker(
            items = supportedDurationValue.map { it.toString() },
            initialIndex = (selectedDuration.value - 1).coerceAtMost(30).toInt(),
            modifier = Modifier
                .weight(6f)
                .padding(horizontal = 6.dp)
        ) {
            currentNumber = supportedDurationValue[it].toLong()
            onChangeDuration(Duration.of(supportedDurationValue[it].toLong(), currentUnit))
        }

        WheelPicker(
            items = supportedDurationTimeUnits.map { it.displayText(resources) },
            initialIndex = supportedDurationTimeUnits.indexOf(selectedDuration.unit),
            modifier = Modifier
                .weight(6f)
                .padding(horizontal = 6.dp)
        ) {
            currentUnit = supportedDurationTimeUnits[it]
            onChangeDuration(Duration.of(currentNumber, supportedDurationTimeUnits[it]))
        }
    }
}


@Composable
private fun ColumnScope.PreviewDescription(preview: String) {
    Text(
        text = preview,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth(),
        style = MaterialTheme.typography.body2
    )
}

private fun previewText(
    resources: Resources,
    @StringRes text: Int,
    calculationMode: TimeIntervalCalculationMode,
    duration: Duration,
): String {
    val timestamp = System.currentTimeMillis() - when (calculationMode) {
        TimeIntervalCalculationMode.PAST   -> TimeInterval.past(duration)
        TimeIntervalCalculationMode.RECENT -> TimeInterval.recently(duration)
    }
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val timeText = formatter.format(timestamp)
    val durationText = duration.displayText(resources, calculationMode.displayText(resources))
    return resources.getString(text, timeText, durationText)
}

private fun previewText(
    resources: Resources,
    @StringRes text: Int,
    duration: Duration,
): String {
    val timestamp = System.currentTimeMillis() + duration.toSeconds() * 1000
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val timeText = formatter.format(timestamp)
    val durationText = duration.displayText(resources, resources.getString(R.string.interval_every))
    return resources.getString(text, timeText, durationText)
}

private const val TAG = "TimeIntervalSettings"