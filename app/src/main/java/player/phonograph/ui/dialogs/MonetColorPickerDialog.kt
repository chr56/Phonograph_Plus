/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import lib.phonograph.misc.ColorPalette
import lib.phonograph.theme.internal.MonetColor
import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.MonetColorPicker
import player.phonograph.util.lifecycleScopeOrNewOne
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.launch

class MonetColorPickerDialog : ComposeViewDialogFragment() {
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
                    positiveButton(
                        res = android.R.string.ok,
                        textStyle = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
                    ) {
                        dismiss()
                    }
                }
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                title(res = R.string.dynamic_colors)
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

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun MonetColorPickerDialogContent(
    mode: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    MonetColorPicker { type: Int, depth: Int ->
        context.lifecycleScopeOrNewOne().launch {
            val palette = MonetColor.MonetColorPalette(type, depth)
            when (mode) {
                ColorPalette.MODE_MONET_PRIMARY_COLOR -> Setting(context)[Keys.monetPalettePrimaryColor].edit { palette.value }
                ColorPalette.MODE_MONET_ACCENT_COLOR  -> Setting(context)[Keys.monetPaletteAccentColor].edit { palette.value }
            }
            onDismiss()
            (context as? Activity)?.recreate()
        }
    }
}