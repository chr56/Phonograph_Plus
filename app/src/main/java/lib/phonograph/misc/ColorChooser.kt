/*
 * Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.misc

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.colorChooser
import player.phonograph.R
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.ThemeSetting
import player.phonograph.util.theme.tintButtons
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N_MR1
import android.os.Build.VERSION_CODES.S


object ColorChooser {

    @SuppressLint("CheckResult")
    fun showColorChooserDialog(context: Context, defaultColor: Int, variant: ColorPalette.Variant) {
        MaterialDialog(context).show {
            title(R.string.pref_header_colors)
            colorChooser(
                colors = ColorPalette.colors,
                subColors = ColorPalette.subColors,
                allowCustomArgb = true,
                initialSelection = defaultColor
            ) { _, color ->
                applyNewColor(context, color, variant)
            }
            if (SDK_INT >= S) {
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
    }

    private fun applyNewColor(context: Context, color: Int, variant: ColorPalette.Variant) {
        when (variant) {
            ColorPalette.Variant.Primary -> Setting(context)[Keys.selectedPrimaryColor].data = color
            ColorPalette.Variant.Accent  -> Setting(context)[Keys.selectedAccentColor].data = color
        }
        ThemeSetting.updateCachedPrimaryColor(context)
        ThemeSetting.updateCachedAccentColor(context)
        if (SDK_INT >= N_MR1) {
            DynamicShortcutManager(context).updateDynamicShortcuts()
        }
    }
}