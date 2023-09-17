/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.model.time.Duration
import player.phonograph.model.time.TimeIntervalCalculationMode
import player.phonograph.model.time.displayText
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.base.BridgeDialogFragment
import player.phonograph.ui.compose.dialogs.PastTimeIntervalPicker
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.debug
import player.phonograph.util.time.TimeInterval.past
import player.phonograph.util.time.TimeInterval.recently
import androidx.compose.foundation.layout.Column
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

class LastAddedPlaylistIntervalDialog : BridgeDialogFragment() {
    @Composable
    override fun Content() {
        val dialogState = rememberMaterialDialogState(true)
        var currentlySelectedMode: TimeIntervalCalculationMode by remember {
            mutableStateOf(Setting.instance.lastAddedCutOffMode)
        }
        var currentlySelected: Duration by remember {
            mutableStateOf(Setting.instance.lastAddedCutOffDuration)
        }


        val flow =
            remember { snapshotFlow { currentlySelectedMode to currentlySelected } }


        val resources = LocalContext.current.resources
        var text by remember { mutableStateOf("") }
        LaunchedEffect(dialogState) {
            flow.collect { (calculationMode, duration) ->
                val cutOff = System.currentTimeMillis() - when (calculationMode) {
                    TimeIntervalCalculationMode.PAST   -> past(duration)
                    TimeIntervalCalculationMode.RECENT -> recently(duration)
                }
                text = previewText(resources, calculationMode, duration, cutOff)
            }
        }

        PhonographTheme {
            MaterialDialog(
                dialogState = dialogState,
                onCloseRequest = { dismiss() },
                buttons = {
                    negativeButton(res = android.R.string.cancel) { dismiss() }
                    positiveButton(res = android.R.string.ok) {
                        dismiss()
                        synchronized(this) {
                            Setting.instance.lastAddedCutOffMode = currentlySelectedMode
                            Setting.instance.lastAddedCutOffDuration = currentlySelected
                        }
                    }
                }
            ) {
                title(res = R.string.pref_title_last_added_interval)
                customView {
                    Column {
                        PastTimeIntervalPicker(
                            currentlySelectedMode,
                            currentlySelected,
                            { calculationMode ->
                                debug { Log.v(TAG, "CalculationMode: ${calculationMode.name}") }
                                currentlySelectedMode = calculationMode
                            },
                            { duration ->
                                debug { Log.v(TAG, "Duration: $duration") }
                                currentlySelected = duration
                            }
                        )
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

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private fun previewText(
        resources: Resources,
        currentlySelectedMode: TimeIntervalCalculationMode,
        currentlySelected: Duration,
        time: Long,
    ): String {
        val timeText = formatter.format(time)
        val resultText = resources.getString(
            R.string.time_interval_text,
            currentlySelectedMode.displayText(resources),
            currentlySelected.value,
            currentlySelected.unit.displayText(resources)
        )
        return resources.getString(R.string.description_time_interval_preview, timeText, resultText)
    }

    companion object {
        private const val TAG = "LastAddedInterval"
    }
}