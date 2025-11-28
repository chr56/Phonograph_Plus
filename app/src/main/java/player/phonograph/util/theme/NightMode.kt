/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.util.debug
import androidx.appcompat.app.AppCompatDelegate
import android.content.res.Configuration
import android.util.Log

private const val TAG = "NightMode"

fun systemNightMode(configuration: Configuration): Boolean? {
    val mode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    // debug { dumpCurrentUiMode(mode) }
    return when (mode) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO  -> false
        else                            -> null
    }
}

fun changeGlobalNightMode(nightMode: Boolean?) {
    // debug { Log.v(TAG, "Switch global night mode: nightMode-$nightMode") }
    AppCompatDelegate.setDefaultNightMode(
        when (nightMode) {
            true  -> AppCompatDelegate.MODE_NIGHT_YES
            false -> AppCompatDelegate.MODE_NIGHT_NO
            null  -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    )
}

fun changeLocalNightMode(delegate: AppCompatDelegate, nightMode: Boolean?) {
    // debug { Log.v(TAG, "Switch global night mode: nightMode-$nightMode") }
    delegate.localNightMode = when (nightMode) {
        true  -> AppCompatDelegate.MODE_NIGHT_YES
        false -> AppCompatDelegate.MODE_NIGHT_NO
        null  -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}


private fun dumpCurrentNightMode(mode: Int) {
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
    Log.v(TAG, "AppCompatDelegate Night Mode: $text")
}

private fun dumpCurrentUiMode(mode: Int) {
    val text = when (mode) {
        Configuration.UI_MODE_NIGHT_YES       -> "YES"
        Configuration.UI_MODE_NIGHT_NO        -> "NO"
        Configuration.UI_MODE_NIGHT_UNDEFINED -> "UNDEFINED"
        else                                  -> "N/A"
    }
    Log.v(TAG, "Resources Night Mode: $text")
}
