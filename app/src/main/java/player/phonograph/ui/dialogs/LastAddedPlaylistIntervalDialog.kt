/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.ui.compose.base.BridgeDialogFragment
import player.phonograph.ui.compose.dialogs.PastTimeIntervalPicker
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.debug
import player.phonograph.util.time.CalculationMode
import player.phonograph.util.time.Duration
import player.phonograph.util.time.past
import player.phonograph.util.time.recently
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
import androidx.compose.ui.unit.dp
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

class LastAddedPlaylistIntervalDialog : BridgeDialogFragment() {
    @Composable
    override fun Content() {
        val dialogState = rememberMaterialDialogState(true)
        var currentlySelectedMode: CalculationMode by remember { mutableStateOf(CalculationMode.PAST) }
        var currentlySelected: Duration by remember { mutableStateOf(Duration.Week(1)) }


        val flow =
            remember { snapshotFlow { currentlySelectedMode to currentlySelected } }

        var time: String by remember { mutableStateOf("NA") }
        LaunchedEffect(dialogState) {
            flow.collect { (calculationMode, duration) ->
                val cutOff = System.currentTimeMillis() - when (calculationMode) {
                    CalculationMode.PAST   -> past(duration)
                    CalculationMode.RECENT -> recently(duration)
                }
                time = formatter.format(cutOff)
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
                            "Last Added Playlist Interval will be after:\n $time",
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

    companion object {
        private const val TAG = "LastAddedInterval"
    }
}