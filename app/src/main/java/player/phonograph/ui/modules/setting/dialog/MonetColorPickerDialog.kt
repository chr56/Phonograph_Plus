/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.ActionItem
import player.phonograph.ui.compose.components.AdvancedDialogFrame
import player.phonograph.ui.compose.components.ColorPalettePicker
import player.phonograph.ui.compose.components.LimitedDialog
import player.phonograph.util.ui.ColorPalette
import player.phonograph.util.ui.MonetColor
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
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

    private fun read(): MonetColor.MonetColorPalette {
        val value = Setting(requireContext())[settingKey].data
        return MonetColor.MonetColorPalette(value)
    }

    private fun save(palette: MonetColor.MonetColorPalette) {
        lifecycleScope.launch {
            Setting(requireContext())[settingKey].edit { palette.value }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = ColorPalette.Variant.valueOf(requireArguments().getString(KEY_MODE)!!)
    }

    @Composable
    override fun Content() {
        PhonographTheme {
            LimitedDialog(onDismiss = ::dismiss) {
                var selected: MonetColor.MonetColorPalette by remember { mutableStateOf(read()) }
                AdvancedDialogFrame(
                    modifier = Modifier,
                    title = stringResource(R.string.dynamic_colors),
                    navigationButtonIcon= rememberVectorPainter(Icons.Default.Close),
                    onDismissRequest = ::dismiss,
                    actions = listOf(
                        ActionItem(
                            Icons.Default.Check,
                            textRes = R.string.action_select,
                            onClick = { save(selected) }
                        )
                    ),
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MonetColorPicker(selected) { selected = it }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Composable
    private fun MonetColorPicker(
        current: MonetColor.MonetColorPalette?,
        onSelected: (MonetColor.MonetColorPalette) -> Unit,
    ) {
        val context = LocalContext.current
        val groupColors = remember {
            ColorPalette.dynamicColors(context).map { Color(it) }
        }
        val allColors = remember {
            ColorPalette.allDynamicColors(context).map { colors -> colors.map { Color(it) } }
        }
        val color = remember(current) {
            Color(current?.color(context) ?: 0)
        }
        ColorPalettePicker(groupColors = groupColors, allColors = allColors, selected = color) { group, order, _ ->
            val type = 1 shl (group + 1)
            val depth = (order + 1) * 100
            onSelected(MonetColor.MonetColorPalette(type, depth))
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