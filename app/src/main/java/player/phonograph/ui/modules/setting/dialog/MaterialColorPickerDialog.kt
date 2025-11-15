/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.setting.dialog

import player.phonograph.R
import player.phonograph.mechanism.PhonographShortcutManager
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.ActionItem
import player.phonograph.ui.compose.components.AdvancedDialogFrame
import player.phonograph.ui.compose.components.ColorPalettePicker
import player.phonograph.ui.compose.components.ColorPicker
import player.phonograph.ui.compose.components.LimitedDialog
import player.phonograph.util.ui.ColorPalette
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle

class MaterialColorPickerDialog : ComposeViewDialogFragment() {

    private lateinit var mode: String
    private var initialColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = requireArguments().getString(KEY_MODE)!!
        initialColor = requireArguments().getInt(KEY_INITIAL_COLOR)
    }

    enum class Page {
        Material, HSV, Monet
    }

    @Composable
    override fun Content() {
        PhonographTheme {
            LimitedDialog(onDismiss = ::dismiss) {
                val context = LocalContext.current
                var colorSelected: Color by remember { mutableStateOf(Color(initialColor)) }
                var page: Page by remember { mutableStateOf(Page.Material) }

                AdvancedDialogFrame(
                    modifier = Modifier,
                    title = stringResource(R.string.pref_header_colors),
                    actions = listOf(
                        ActionItem(
                            imageRes = R.drawable.ic_edit_white_24dp,
                            textRes = R.string.pref_category_advanced,
                            onClick = {
                                page = if (SDK_INT >= VERSION_CODES.S) {
                                    when (page) {
                                        Page.Material -> Page.HSV
                                        Page.HSV      -> Page.Monet
                                        Page.Monet    -> Page.Material
                                    }
                                } else {
                                    when (page) {
                                        Page.Material -> Page.HSV
                                        else          -> Page.Material
                                    }
                                }
                            }
                        ),
                        ActionItem(
                            Icons.Default.Check,
                            textRes = R.string.action_select,
                            onClick = {
                                applyNewColor(
                                    context,
                                    colorSelected.toArgb(),
                                    ColorPalette.Variant.valueOf(mode)
                                )
                            }
                        )
                    ),
                    onDismissRequest = ::dismiss
                ) {
                    when (page) {
                        Page.Material -> {
                            ColorPalettePicker(
                                groupColors = ColorPalette.colors.map { Color(it) },
                                allColors = ColorPalette.subColors.map { colors -> colors.map { Color(it) } },
                                selected = colorSelected,
                            ) { _, _, color -> colorSelected = color }
                        }

                        Page.Monet    -> {
                            if (SDK_INT >= VERSION_CODES.S) {
                                ColorPalettePicker(
                                    groupColors = ColorPalette.dynamicColors(context).map { Color(it) },
                                    allColors = ColorPalette.allDynamicColors(context)
                                        .map { colors -> colors.map { Color(it) } },
                                    selected = colorSelected,
                                ) { _, _, color -> colorSelected = color }
                            }
                        }

                        Page.HSV      -> {
                            ColorPicker(
                                selected = colorSelected,
                                modifier = Modifier.padding(32.dp)
                            ) { colorSelected = it }
                        }
                    }
                }
            }
        }
    }


    private fun applyNewColor(context: Context, color: Int, variant: ColorPalette.Variant) {
        when (variant) {
            ColorPalette.Variant.Primary -> Setting(context)[Keys.selectedPrimaryColor].data = color
            ColorPalette.Variant.Accent  -> Setting(context)[Keys.selectedAccentColor].data = color
        }
        if (SDK_INT >= VERSION_CODES.N_MR1) {
            PhonographShortcutManager.updateDynamicShortcuts(context)
        }
    }

    companion object {
        private const val KEY_MODE = "mode"
        private const val KEY_INITIAL_COLOR = "color"

        private fun create(variant: ColorPalette.Variant, initialColor: Int): MaterialColorPickerDialog =
            MaterialColorPickerDialog().apply {
                arguments = Bundle().apply {
                    putString(KEY_MODE, variant.name)
                    putInt(KEY_INITIAL_COLOR, initialColor)
                }
            }

        fun showColorChooserDialog(context: Context, initialColor: Int, variant: ColorPalette.Variant) =
            create(variant, initialColor).show((context as FragmentActivity).supportFragmentManager, null)
    }
}