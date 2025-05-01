/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import androidx.annotation.StringDef


//region Values
// Theme
const val THEME_AUTO_LIGHTBLACK = "auto_lightblack"
const val THEME_AUTO_LIGHTDARK = "auto_lightdark"
const val THEME_LIGHT = "light"
const val THEME_BLACK = "black"
const val THEME_DARK = "dark"

@StringDef(THEME_AUTO_LIGHTBLACK, THEME_AUTO_LIGHTDARK, THEME_LIGHT, THEME_BLACK, THEME_DARK)
@Retention(AnnotationRetention.SOURCE)
annotation class GeneralTheme
//endregion