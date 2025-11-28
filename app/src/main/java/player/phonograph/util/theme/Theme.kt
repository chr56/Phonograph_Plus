/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.model.ui.GeneralTheme.Companion.THEME_BLACK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_DARK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_LIGHT
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import android.content.Context

@JvmName("Context_PrimaryColor")
@CheckResult
@ColorInt
fun Context.primaryColor(): Int = ThemeSettingsDelegate.currentPrimaryColor()

@JvmName("Fragment_PrimaryColor")
@CheckResult
@ColorInt
fun Fragment.primaryColor(): Int = ThemeSettingsDelegate.currentPrimaryColor()

@JvmName("Context_AccentColor")
@CheckResult
@ColorInt
fun Context.accentColor(): Int = ThemeSettingsDelegate.currentAccentColor()

@JvmName("Fragment_AccentColor")
@CheckResult
@ColorInt
fun Fragment.accentColor(): Int = ThemeSettingsDelegate.currentAccentColor()

val Context.nightMode: Boolean get() = ThemeSettingsDelegate.isNightTheme(this)


suspend fun toggleTheme(context: Context): Boolean {
    val preference = Setting(context)[Keys.theme]
    val oldTheme = preference.read()
    val newTheme = when (oldTheme) {
        THEME_DARK, THEME_BLACK -> THEME_LIGHT
        THEME_LIGHT             -> THEME_DARK
        else                    -> null
    }
    return if (newTheme != null) {
        preference.edit { newTheme }
        true
    } else {
        false
    }
}

