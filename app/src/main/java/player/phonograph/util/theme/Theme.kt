/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.theme

import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.settings.Setting
import player.phonograph.mechanism.setting.StyleConfig
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import mt.color.R as MR


/**
 * adjust color settings from Dynamic Color of Material You if available
 */
fun applyMonet(context: Context, force: Boolean = false) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (force || Setting.instance.enableMonet)) {
        ThemeColor.editTheme(context)
            .primaryColor(context.getColor(android.R.color.system_accent1_400))
            .accentColor(context.getColor(android.R.color.system_accent1_700))
            .commit()
    }
}

val Context.nightMode: Boolean get() = StyleConfig.isNightMode(this)

fun backgroundColorByTheme(context: Context): Int = context.resources.getColor(
    when (StyleConfig.generalTheme(context)) {
        R.style.Theme_Phonograph_Auto  -> R.color.cardBackgroundColor
        R.style.Theme_Phonograph_Light -> MR.color.md_white_1000
        R.style.Theme_Phonograph_Black -> MR.color.md_black_1000
        R.style.Theme_Phonograph_Dark  -> MR.color.md_grey_800
        else                           -> MR.color.md_grey_700
    },
    context.theme
)

