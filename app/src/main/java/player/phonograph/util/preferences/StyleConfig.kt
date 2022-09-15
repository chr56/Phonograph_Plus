/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.preferences

import androidx.annotation.StyleRes
import player.phonograph.R
import player.phonograph.settings.Setting

object StyleConfig {

    @get:StyleRes
    val generalTheme: Int
        get() = getThemeResFromPrefValue(
            Setting.instance.themeString
        )

    fun setGeneralTheme(theme: String) {
        Setting.instance.themeString = theme
    }

    @StyleRes
    fun getThemeResFromPrefValue(themePrefValue: String?): Int {
        return when (themePrefValue) {
            "dark" -> R.style.Theme_Phonograph_Dark
            "black" -> R.style.Theme_Phonograph_Black
            "light" -> R.style.Theme_Phonograph_Light
            "auto" -> R.style.Theme_Phonograph_Auto
            else -> R.style.Theme_Phonograph_Auto
        }
    }
}