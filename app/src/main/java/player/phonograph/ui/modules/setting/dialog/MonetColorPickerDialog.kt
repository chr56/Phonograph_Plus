/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.MonetColorPicker
import player.phonograph.util.concurrent.lifecycleScopeOrNewOne
import player.phonograph.util.theme.accentColoredButtonStyle
import player.phonograph.util.ui.ColorPalette
import player.phonograph.util.ui.MonetColor
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.launch

class MonetColorPickerDialog : ComposeViewDialogFragment() {

    private lateinit var mode: ColorPalette.Variant

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = ColorPalette.Variant.valueOf(requireArguments().getString(KEY_MODE)!!)
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
                        textStyle = accentColoredButtonStyle()
                    ) {
                        dismiss()
                    }
                }
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                title(res = R.string.dynamic_colors)
                customView {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val context = LocalContext.current
                        MonetColorPicker { type: Int, depth: Int ->
                            context.lifecycleScopeOrNewOne().launch {
                                val palette = MonetColor.MonetColorPalette(type, depth)
                                val key = when (mode) {
                                    ColorPalette.Variant.Primary -> Keys.monetPalettePrimaryColor
                                    ColorPalette.Variant.Accent  -> Keys.monetPaletteAccentColor
                                }
                                Setting(context)[key].edit { palette.value }
                                dismiss()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_MODE = "mode"

        private fun create(variant: ColorPalette.Variant): MonetColorPickerDialog =
            MonetColorPickerDialog().apply {
                arguments = Bundle().apply {
                    putString(KEY_MODE, variant.name)
                }
            }

        fun showColorChooserDialog(context: Context, variant: ColorPalette.Variant) =
            create(variant).show((context as FragmentActivity).supportFragmentManager, null)
    }
}