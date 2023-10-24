/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.util.debug
import androidx.appcompat.app.AppCompatDelegate
import android.content.res.Configuration
import android.util.Log

private const val TAG = "NightMode"

inline fun checkNightMode(config: Configuration, block: (Boolean) -> Unit) {
    val mode: Int = config.uiMode and Configuration.UI_MODE_NIGHT_MASK
    debug { displayCurrentUiMode(mode) }
    when (mode) {
        Configuration.UI_MODE_NIGHT_NO        -> block(false)
        Configuration.UI_MODE_NIGHT_YES       -> block(true)
        Configuration.UI_MODE_NIGHT_UNDEFINED -> Log.i("checkNightMode", "unspecified night mode")
    }
}

fun displayCurrentNightMode(mode: Int) {
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


fun changeGlobalNightMode(nightMode: Boolean) {
    debug { Log.v(TAG, "switch global night mode : $nightMode") }
    AppCompatDelegate.setDefaultNightMode(
        if (nightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
    )
}

fun changeLocalNightMode(delegate: AppCompatDelegate, nightMode: Boolean) {
    debug { Log.v(TAG, "switch local night mode : $nightMode") }
    delegate.localNightMode =
        if (nightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
}

