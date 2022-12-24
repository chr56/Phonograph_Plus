/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.preferences

import player.phonograph.R
import player.phonograph.settings.Setting
import androidx.annotation.StyleRes
import android.content.Context

object StyleConfig {

    @StyleRes
    fun generalTheme(context: Context): Int =
        getThemeResFromPrefValue(Setting.instance(context).themeString)

    fun setGeneralTheme(theme: String) {
        Setting.instance.themeString = theme
    }

    @StyleRes
    fun getThemeResFromPrefValue(themePrefValue: String?): Int {
        return when (themePrefValue) {
            THEME_AUTO  -> R.style.Theme_Phonograph_Auto
            THEME_DARK  -> R.style.Theme_Phonograph_Dark
            THEME_BLACK -> R.style.Theme_Phonograph_Black
            THEME_LIGHT -> R.style.Theme_Phonograph_Light
            else        -> R.style.Theme_Phonograph_Auto
        }
    }

    const val THEME_AUTO = "auto"
    const val THEME_DARK = "dark"
    const val THEME_BLACK = "black"
    const val THEME_LIGHT = "light"
}