/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.settings

import lib.phonograph.misc.MonetColor
import player.phonograph.util.theme.parseToStyleRes
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES

object ThemeSetting {

    @CheckResult
    @StyleRes
    fun themeStyle(context: Context): Int =
        parseToStyleRes(cachedTheme ?: updateThemeStyle(context))

    private var cachedTheme: String? = null

    fun updateThemeStyle(context: Context): String {
        val theme = Setting(context)[Keys.theme].data
        cachedTheme = theme
        return theme
    }

    @CheckResult
    @ColorInt
    fun primaryColor(context: Context): Int =
        if (!isPrimaryColorUpdated) updateCachedPrimaryColor(context) else cachedPrimaryColor

    @CheckResult
    @ColorInt
    fun accentColor(context: Context): Int =
        if (!isAccentColorUpdated) updateCachedAccentColor(context) else cachedAccentColor

    private var isPrimaryColorUpdated = false
    private var isAccentColorUpdated = false


    @ColorInt
    private var cachedPrimaryColor: Int = 0

    @ColorInt
    private var cachedAccentColor: Int = 0

    /**
     * update cached color and return latest
     */
    @ColorInt
    fun updateCachedPrimaryColor(context: Context): Int {
        val setting = Setting(context)
        val primaryColor =
            if (SDK_INT >= VERSION_CODES.S && setting[Keys.enableMonet].data) {
                MonetColor.MonetColorPalette(
                    Setting(context)[Keys.monetPalettePrimaryColor].data
                ).color(context)
            } else {
                Setting(context)[Keys.selectedPrimaryColor].data
            }
        cachedPrimaryColor = primaryColor
        isPrimaryColorUpdated = true
        return primaryColor
    }

    /**
     * update cached color and return latest
     */
    @ColorInt
    fun updateCachedAccentColor(context: Context): Int {
        val setting = Setting(context)
        val primaryColor =
            if (SDK_INT >= VERSION_CODES.S && setting[Keys.enableMonet].data) {
                MonetColor.MonetColorPalette(
                    Setting(context)[Keys.monetPaletteAccentColor].data
                ).color(context)
            } else {
                Setting(context)[Keys.selectedAccentColor].data
            }
        cachedAccentColor = primaryColor
        isAccentColorUpdated = true
        return primaryColor
    }

    fun peekCachedPrimaryColor() = cachedPrimaryColor
    fun peekCachedAccentColor() = cachedAccentColor
}