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
import player.phonograph.ui.compose.components.ColorPalettePicker
import player.phonograph.util.concurrent.lifecycleScopeOrNewOne
import player.phonograph.util.theme.accentColoredButtonStyle
import player.phonograph.util.ui.ColorPalette
import player.phonograph.util.ui.MonetColor
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.launch

class MonetColorPickerDialog : ComposeViewDialogFragment() {

    private lateinit var mode: ColorPalette.Variant
    private val settingKey
        get() = when (mode) {
            ColorPalette.Variant.Primary -> Keys.monetPalettePrimaryColor
            ColorPalette.Variant.Accent  -> Keys.monetPaletteAccentColor
        }

    private fun read(context: Context): Color {
        val value = Setting(context)[settingKey].data
        return Color(MonetColor.MonetColorPalette(value).color(context))
    }

    private fun save(context: Context, type: Int, depth: Int) {
        context.lifecycleScopeOrNewOne().launch {
            val palette = MonetColor.MonetColorPalette(type, depth)
            Setting(context)[settingKey].edit { palette.value }
            dismiss()
        }
    }


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
                        val current = read(context)
                        MonetColorPicker(current) { type, depth -> save(context, type, depth) }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Composable
    private fun MonetColorPicker(
        current: Color?,
        onSelected: (Int, Int) -> Unit,
    ) {
        val context = LocalContext.current
        val groupColors = remember {
            ColorPalette.dynamicColors(context).map { Color(it) }
        }
        val allColors = remember {
            ColorPalette.allDynamicColors(context).map { colors -> colors.map { Color(it) } }
        }
        ColorPalettePicker(groupColors = groupColors, allColors = allColors, selected = current) { group, order, _ ->
            val type = 1 shl (group + 1)
            val depth = (order + 1) * 100
            onSelected(type, depth)
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