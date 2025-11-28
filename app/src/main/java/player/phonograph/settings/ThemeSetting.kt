/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.settings

import player.phonograph.R
import player.phonograph.model.ui.GeneralTheme
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_AUTO_LIGHTBLACK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_AUTO_LIGHTDARK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_BLACK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_DARK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_LIGHT
import player.phonograph.util.theme.parseToStyleRes
import player.phonograph.util.ui.MonetColor
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES

object ThemeSetting {

    private var cachedTheme: String? = null

    @CheckResult
    @GeneralTheme
    fun theme(context: Context): String = cachedTheme ?: updateThemeStyleCache(context)

    @CheckResult
    @StyleRes
    fun themeStyle(context: Context): Int = parseToStyleRes(theme(context))

    @StyleRes
    fun parseToStyleRes(@GeneralTheme theme: String): Int = when (theme) {
        THEME_AUTO_LIGHTBLACK -> R.style.Theme_Phonograph_Auto_LightBlack
        THEME_AUTO_LIGHTDARK  -> R.style.Theme_Phonograph_Auto_LightDark
        THEME_LIGHT           -> R.style.Theme_Phonograph_Light
        THEME_BLACK           -> R.style.Theme_Phonograph_Black
        THEME_DARK            -> R.style.Theme_Phonograph_Dark
        else                  -> R.style.Theme_Phonograph_Auto_LightBlack
    }

    fun updateThemeStyleCache(context: Context): String {
        val theme = Setting(context)[Keys.theme].data
        return updateThemeStyleCache(theme)
    }

    fun updateThemeStyleCache(theme: String): String {
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

}