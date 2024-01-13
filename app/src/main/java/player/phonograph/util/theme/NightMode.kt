/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.util.debug
import androidx.appcompat.app.AppCompatDelegate
import android.content.res.Configuration
import android.util.Log

private const val TAG = "NightMode"

inline fun checkNightMode(config: Configuration, block: (present: Boolean, night: Boolean) -> Unit) {
    val mode: Int = config.uiMode and Configuration.UI_MODE_NIGHT_MASK
    debug { displayCurrentUiMode(mode) }
    when (mode) {
        Configuration.UI_MODE_NIGHT_NO        -> block(true, false)
        Configuration.UI_MODE_NIGHT_YES       -> block(true, true)
        Configuration.UI_MODE_NIGHT_UNDEFINED -> block(false, false)
    }
}

fun displayCurrentNightMode(mode: Int) {
    @Suppress("DEPRECATION")
    val text = when (mode) {
        AppCompatDelegate.MODE_NIGHT_YES           -> "YES"
        AppCompatDelegate.MODE_NIGHT_NO            -> "NO"
        AppCompatDelegate.MODE_NIGHT_UNSPECIFIED   -> "UNSPECIFIED"
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> "FOLLOW_SYSTEM"
        AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY  -> "AUTO_BATTERY"
        AppCompatDelegate.MODE_NIGHT_AUTO_TIME     -> "AUTO_TIME"
        else                                       -> "NA"
    }
    Log.v(TAG, "Night Mode is $text")
}

fun displayCurrentUiMode(mode: Int) {
    val text = when (mode) {
        Configuration.UI_MODE_NIGHT_YES       -> "YES"
        Configuration.UI_MODE_NIGHT_NO        -> "NO"
        Configuration.UI_MODE_NIGHT_UNDEFINED -> "UNDEFINED"
        else                                  -> "NA"
    }
    Log.v(TAG, "Night Mode is $text")
}


fun changeGlobalNightMode(present: Boolean, nightMode: Boolean) {
    debug { Log.v(TAG, "switch global night mode : present-$present, nightMode-$nightMode") }
    AppCompatDelegate.setDefaultNightMode(
        if (present) {
            if (nightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    )
}

fun changeLocalNightMode(delegate: AppCompatDelegate, present: Boolean, nightMode: Boolean) {
    debug { Log.v(TAG, "switch local night mode : present-$present, nightMode-$nightMode") }
    delegate.localNightMode =
        if (present) {
            if (nightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
}

