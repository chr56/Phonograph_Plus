/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import lib.phonograph.misc.ColorPalette
import player.phonograph.ui.compose.base.BridgeDialogFragment
import player.phonograph.ui.compose.dialogs.MonetColorPickerDialogContent
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.os.Build
import android.os.Bundle

class MonetColorPickerDialog : BridgeDialogFragment() {
    private var mode: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = requireArguments().getInt(KEY_MODE)
    }

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
                title(res = player.phonograph.R.string.pref_header_colors)
                customView {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MonetColorPickerDialogContent(mode = mode, onDismiss = ::dismiss)
                    }
                }
            }
        }
    }

    companion object {
        const val KEY_MODE = "mode"

        fun primaryColor() = create(ColorPalette.MODE_MONET_PRIMARY_COLOR)
        fun accentColor() = create(ColorPalette.MODE_MONET_ACCENT_COLOR)

        private fun create(mode: Int): MonetColorPickerDialog =
            MonetColorPickerDialog().apply {
                arguments = Bundle().apply {
                    putInt(KEY_MODE, mode)
                }
            }
    }
}