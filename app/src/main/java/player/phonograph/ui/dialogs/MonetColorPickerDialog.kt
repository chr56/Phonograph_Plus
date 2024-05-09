/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import lib.phonograph.misc.ColorPalette
import lib.phonograph.misc.MonetColor
import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.ThemeSetting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.MonetColorPicker
import player.phonograph.util.lifecycleScopeOrNewOne
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.annotation.RequiresApi
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

    private lateinit var mode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = requireArguments().getString(KEY_MODE)!!
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
                        MonetColorPickerDialogContent(ColorPalette.Variant.valueOf(mode), onDismiss = ::dismiss)
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

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun MonetColorPickerDialogContent(
    variant: ColorPalette.Variant,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    MonetColorPicker { type: Int, depth: Int ->
        context.lifecycleScopeOrNewOne().launch {
            val palette = MonetColor.MonetColorPalette(type, depth)
            when (variant) {
                ColorPalette.Variant.Primary -> Setting(context)[Keys.monetPalettePrimaryColor].edit { palette.value }
                ColorPalette.Variant.Accent  -> Setting(context)[Keys.monetPaletteAccentColor].edit { palette.value }
            }
            ThemeSetting.updateCachedPrimaryColor(context)
            ThemeSetting.updateCachedAccentColor(context)
            onDismiss()
        }
    }
}