/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.setting

import player.phonograph.R
import player.phonograph.settings.Setting
import androidx.annotation.StyleRes
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources

object StyleConfig {

    @StyleRes
    fun generalTheme(context: Context): Int =
        getThemeResFromPrefValue(Setting.instance.themeString)

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

    fun isNightMode(context: Context): Boolean =
        when (Setting.instance.themeString) {
            THEME_DARK  -> true
            THEME_BLACK -> true
            THEME_LIGHT -> false
            THEME_AUTO  -> systemDarkmode(context.resources)
            else        -> false
        }

    fun systemDarkmode(resources: Resources):Boolean =
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO  -> false
            else                            -> false
        }

    const val THEME_AUTO = "auto"
    const val THEME_DARK = "dark"
    const val THEME_BLACK = "black"
    const val THEME_LIGHT = "light"
}