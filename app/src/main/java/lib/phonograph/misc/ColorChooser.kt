/*
 * Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.misc

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.color.colorChooser
import player.phonograph.R
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.ThemeSetting.accentColor
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N_MR1
import android.os.Build.VERSION_CODES.S


object ColorChooser {

    @SuppressLint("CheckResult")
    fun showColorChooserDialog(context: Context, defaultColor: Int, mode: Int) {
        MaterialDialog(context).show {
            title(R.string.pref_header_colors)
            colorChooser(
                colors = ColorPalette.colors, subColors = ColorPalette.subColors, allowCustomArgb = true, initialSelection = defaultColor
            ) { _, color ->
                applyNewColor(context, color, mode)
            }
            val accentColor = accentColor(context)
            if (SDK_INT >= S) {
                @Suppress("DEPRECATION")
                neutralButton(res = R.string.dynamic_colors) {
                    MaterialDialog(context).title(R.string.dynamic_colors).colorChooser(
                        colors = ColorPalette.dynamicColors(context), subColors = ColorPalette.allDynamicColors(context)
                    ) { _, color ->
                        applyNewColor(context, color, mode)
                    }.positiveButton {
                        it.dismiss()
                        dismiss()
                    }.negativeButton {
                        it.dismiss()
                    }.apply {
                        getActionButton(WhichButton.POSITIVE).updateTextColor(accentColor)
                        getActionButton(WhichButton.NEGATIVE).updateTextColor(accentColor)
                    }.show()
                }
            }
            positiveButton(res = android.R.string.ok)
            negativeButton(res = android.R.string.cancel).apply {
                // set button color
                getActionButton(WhichButton.POSITIVE).updateTextColor(accentColor)
                getActionButton(WhichButton.NEGATIVE).updateTextColor(accentColor)
                getActionButton(WhichButton.NEUTRAL).updateTextColor(accentColor)
            }
        }
    }

    private fun applyNewColor(context: Context, color: Int, mode: Int) {
        when (mode) {
            ColorPalette.MODE_PRIMARY_COLOR -> Setting(context)[Keys.selectedPrimaryColor].data = color
            ColorPalette.MODE_ACCENT_COLOR  -> Setting(context)[Keys.selectedAccentColor].data = color
            0                               -> return
        }
        if (SDK_INT >= N_MR1) {
            DynamicShortcutManager(context).updateDynamicShortcuts()
        }
        (context as? Activity)?.recreate()
    }
}