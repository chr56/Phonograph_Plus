/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.preferences

import player.phonograph.App
import androidx.annotation.StyleRes
import player.phonograph.R
import player.phonograph.settings.Setting
import android.content.Context

object StyleConfig {

    @get:StyleRes
    val generalTheme: Int
        get() = generalTheme(App.instance)

    @StyleRes
    fun generalTheme(context: Context): Int =
        getThemeResFromPrefValue(Setting.instance(context).themeString)

    fun setGeneralTheme(theme: String) {
        Setting.instance.themeString = theme
    }

    @StyleRes
    fun getThemeResFromPrefValue(themePrefValue: String?): Int {
        return when (themePrefValue) {
            "dark"  -> R.style.Theme_Phonograph_Dark
            "black" -> R.style.Theme_Phonograph_Black
            "light" -> R.style.Theme_Phonograph_Light
            "auto"  -> R.style.Theme_Phonograph_Auto
            else    -> R.style.Theme_Phonograph_Auto
        }
    }
}