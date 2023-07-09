/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.mechanism.setting.StyleConfig
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources

val Context.nightMode: Boolean get() = isNightMode(this)

private fun isNightMode(context: Context): Boolean =
    when (StyleConfig.generalTheme(context)) {
        StyleConfig.THEME_DARK  -> true
        StyleConfig.THEME_BLACK -> true
        StyleConfig.THEME_LIGHT -> false
        StyleConfig.THEME_AUTO  -> systemDarkmode(context.resources)
        else                    -> false
    }

fun systemDarkmode(resources: Resources): Boolean =
    when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO  -> false
        else                            -> false
    }