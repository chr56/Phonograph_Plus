/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.ui

import androidx.annotation.StringDef

@StringDef(
    GeneralTheme.THEME_AUTO_LIGHTBLACK,
    GeneralTheme.THEME_AUTO_LIGHTDARK,
    GeneralTheme.THEME_LIGHT,
    GeneralTheme.THEME_BLACK,
    GeneralTheme.THEME_DARK
)
@Retention(AnnotationRetention.SOURCE)
annotation class GeneralTheme {
    companion object {
        const val THEME_AUTO_LIGHTBLACK = "auto_lightblack"
        const val THEME_AUTO_LIGHTDARK = "auto_lightdark"
        const val THEME_LIGHT = "light"
        const val THEME_BLACK = "black"
        const val THEME_DARK = "dark"
    }
}