/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeUnit
import player.phonograph.model.time.displayText
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.WheelPicker
import player.phonograph.util.debug
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.res.Resources
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

class CheckUpdateIntervalDialog : ComposeViewDialogFragment() {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        var duration: Duration by remember {
            mutableStateOf(Setting(context).Composites[Keys.checkUpdateInterval].data)
        }
        PhonographTheme {
            val dialogState = rememberMaterialDialogState(true)
            var text by remember { mutableStateOf("") }
            val flow = snapshotFlow { duration }
            val resources = context.resources
            LaunchedEffect(dialogState) {
                flow.collect {
                    text = previewText(resources, it)
                }
            }
            MaterialDialog(
                dialogState = dialogState,
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
                            Setting(App.instance).Composites[Keys.checkUpdateInterval].data = duration
                        }
                    }
                }
            ) {
                title(res = R.string.pref_title_check_for_updates_interval)
                customView {
                    Column {
                        TimeIntervalPicker(duration) {
                            duration = it
                            debug { Log.v(TAG, it.toString()) }
                        }
                        Text(
                            text,
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private fun previewText(
        resources: Resources,
        duration: Duration,
    ): String {
        val timeText = formatter.format(System.currentTimeMillis() + duration.toSeconds() * 1000)
        val durationText = duration.displayText(resources, resources.getString(R.string.interval_every))
        return resources.getString(R.string.preview_text_next_updates_check, timeText, durationText)
    }

    companion object {
        private const val TAG = "CheckUpgradeInterval"
    }
}

@Composable
private fun TimeIntervalPicker(
    selected: Duration,
    modifier: Modifier = Modifier,
    onChangeDuration: (Duration) -> Unit,
) {
    val resources = LocalContext.current.resources
    val units = remember {
        listOf(TimeUnit.Week, TimeUnit.Day, TimeUnit.Hour)
    }
    val numbers = remember { (0..24).toList() }

    var currentNumber by remember { mutableStateOf(selected.value) }
    var currentUnit by remember { mutableStateOf(selected.unit) }

    Row(
        modifier,
        Arrangement.SpaceBetween
    ) {
        WheelPicker(
            items = numbers.map { it.toString() },
            initialIndex = selected.value.coerceAtMost(24).toInt(),
            modifier = Modifier
                .weight(6f)
                .padding(horizontal = 6.dp)
        ) {
            currentNumber = numbers[it].toLong()
            onChangeDuration(Duration.of(numbers[it].toLong(), currentUnit))
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