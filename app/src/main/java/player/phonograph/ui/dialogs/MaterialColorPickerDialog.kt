/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.colorChooser
import lib.phonograph.misc.ColorPalette
import player.phonograph.R
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.theme.tintButtons
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle

class MaterialColorPickerDialog : DialogFragment() {

    private lateinit var mode: String
    private var initialColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = requireArguments().getString(KEY_MODE)!!
        initialColor = requireArguments().getInt(KEY_INITIAL_COLOR)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        colorPicker(requireContext(), initialColor, ColorPalette.Variant.valueOf(mode))

    @SuppressLint("CheckResult")
    private fun colorPicker(context: Context, defaultColor: Int, variant: ColorPalette.Variant): Dialog =
        MaterialDialog(context).apply {
            title(R.string.pref_header_colors)
            colorChooser(
                colors = ColorPalette.colors,
                subColors = ColorPalette.subColors,
                allowCustomArgb = true,
                initialSelection = defaultColor
            ) { _, color ->
                applyNewColor(context, color, variant)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                @Suppress("DEPRECATION")
                neutralButton(res = R.string.dynamic_colors) {
                    MaterialDialog(context).title(R.string.dynamic_colors)
                        .colorChooser(
                            colors = ColorPalette.dynamicColors(context),
                            subColors = ColorPalette.allDynamicColors(context)
                        ) { _, color ->
                            applyNewColor(context, color, variant)
                        }.positiveButton {
                            it.dismiss()
                            dismiss()
                        }.negativeButton {
                            it.dismiss()
                        }.tintButtons().show()
                }
            }
            positiveButton(res = android.R.string.ok)
            negativeButton(res = android.R.string.cancel)
            tintButtons()
        }

    private fun applyNewColor(context: Context, color: Int, variant: ColorPalette.Variant) {
        when (variant) {
            ColorPalette.Variant.Primary -> Setting(context)[Keys.selectedPrimaryColor].data = color
            ColorPalette.Variant.Accent  -> Setting(context)[Keys.selectedAccentColor].data = color
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            DynamicShortcutManager(context).updateDynamicShortcuts()
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