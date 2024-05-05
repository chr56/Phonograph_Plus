/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.App
import player.phonograph.R
import player.phonograph.settings.GeneralTheme
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.THEME_AUTO
import player.phonograph.settings.THEME_BLACK
import player.phonograph.settings.THEME_DARK
import player.phonograph.settings.THEME_LIGHT
import player.phonograph.settings.ThemeSetting
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources

@JvmName("Context_PrimaryColor")
@CheckResult
@ColorInt
fun Context.primaryColor(): Int = ThemeSetting.primaryColor(this)

@JvmName("Fragment_PrimaryColor")
@CheckResult
@ColorInt
fun Fragment.primaryColor(): Int = ThemeSetting.primaryColor(context ?: App.instance)

@JvmName("Context_AccentColor")
@CheckResult
@ColorInt
fun Context.accentColor(): Int = ThemeSetting.accentColor(this)

@JvmName("Fragment_AccentColor")
@CheckResult
@ColorInt
fun Fragment.accentColor(): Int = ThemeSetting.accentColor(context ?: App.instance)

val Context.nightMode: Boolean get() = isNightMode(this)

private fun isNightMode(context: Context): Boolean =
    when (Setting(context)[Keys.theme].data) {
        THEME_DARK  -> true
        THEME_BLACK -> true
        THEME_LIGHT -> false
        THEME_AUTO  -> systemDarkmode(context.resources)
        else        -> false
    }

fun systemDarkmode(resources: Resources): Boolean =
    when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO  -> false
        else                            -> false
    }


@StyleRes
private fun parseToStyleRes(@GeneralTheme theme: String): Int =
    when (theme) {
        THEME_AUTO  -> R.style.Theme_Phonograph_Auto
        THEME_DARK  -> R.style.Theme_Phonograph_Dark
        THEME_BLACK -> R.style.Theme_Phonograph_Black
        THEME_LIGHT -> R.style.Theme_Phonograph_Light
        else        -> R.style.Theme_Phonograph_Auto
    }

fun toggleTheme(context: Context): Boolean {
    val preference = Setting(context)[Keys.theme]
    val theme = preference.data
    return if (theme != THEME_AUTO) {
        when (theme) {
            THEME_DARK, THEME_BLACK -> preference.data = THEME_LIGHT
            THEME_LIGHT             -> preference.data = THEME_DARK
        }
        true
    } else {
        false
    }
}
