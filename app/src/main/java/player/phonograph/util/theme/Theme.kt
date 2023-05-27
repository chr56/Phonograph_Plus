/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.theme

import mt.pref.ThemeColor
import player.phonograph.mechanism.setting.StyleConfig
import player.phonograph.settings.Setting
import android.content.Context
import android.os.Build


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