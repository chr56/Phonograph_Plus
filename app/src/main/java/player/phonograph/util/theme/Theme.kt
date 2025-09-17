/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.ui.GeneralTheme
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_AUTO_LIGHTBLACK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_AUTO_LIGHTDARK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_BLACK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_DARK
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_LIGHT
import player.phonograph.settings.Keys
import player.phonograph.settings.PrimitiveKey
import player.phonograph.settings.Setting
import player.phonograph.settings.ThemeSetting
import player.phonograph.util.ui.MonetColor
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

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
        THEME_DARK                                  -> true
        THEME_BLACK                                 -> true
        THEME_LIGHT                                 -> false
        THEME_AUTO_LIGHTBLACK, THEME_AUTO_LIGHTDARK -> systemDarkmode(context.resources)
        else                                        -> false
    }

fun systemDarkmode(resources: Resources): Boolean = systemDarkmode(resources.configuration)

fun systemDarkmode(configuration: Configuration): Boolean =
    when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO  -> false
        else                            -> false
    }

fun currentActualTheme(context: Context, default: String): Flow<String> =
    Setting(context)[Keys.theme].flow.map {
        when (it) {
            THEME_AUTO_LIGHTBLACK -> if (systemDarkmode(context.resources)) THEME_BLACK else THEME_LIGHT
            THEME_AUTO_LIGHTDARK  -> if (systemDarkmode(context.resources)) THEME_DARK else THEME_LIGHT
            THEME_LIGHT           -> THEME_LIGHT
            THEME_BLACK           -> THEME_BLACK
            THEME_DARK            -> THEME_DARK
            else                  -> default
        }
    }

@StyleRes
fun parseToStyleRes(@GeneralTheme theme: String): Int =
    when (theme) {
        THEME_AUTO_LIGHTBLACK -> R.style.Theme_Phonograph_Auto_LightBlack
        THEME_AUTO_LIGHTDARK  -> R.style.Theme_Phonograph_Auto_LightDark
        THEME_LIGHT           -> R.style.Theme_Phonograph_Light
        THEME_BLACK           -> R.style.Theme_Phonograph_Black
        THEME_DARK            -> R.style.Theme_Phonograph_Dark
        else                  -> R.style.Theme_Phonograph_Auto_LightBlack
    }

fun toggleTheme(context: Context): Boolean {
    val preference = Setting(context)[Keys.theme]
    val theme = preference.data
    return if (theme != THEME_AUTO_LIGHTBLACK && theme != THEME_AUTO_LIGHTDARK) {
        when (theme) {
            THEME_DARK, THEME_BLACK -> preference.data = THEME_LIGHT
            THEME_LIGHT             -> preference.data = THEME_DARK
        }
        true
    } else {
        false
    }
}

private fun colorFlow(context: Context, monetPalette: PrimitiveKey<Int>, selected: PrimitiveKey<Int>): Flow<Int> {
    val preferencesFlow = Setting(context).dataStore.data
    return preferencesFlow.map { preference ->
        val enableMonet = preference[Keys.enableMonet.preferenceKey] ?: Keys.enableMonet.defaultValue()
        if (SDK_INT >= VERSION_CODES.S && enableMonet) {
            MonetColor.MonetColorPalette(
                preference[monetPalette.preferenceKey] ?: monetPalette.defaultValue()
            ).color(context)
        } else {
            preference[selected.preferenceKey] ?: selected.defaultValue()
        }
    }
}

fun primaryColorFlow(context: Context): Flow<Int> =
    colorFlow(context, Keys.monetPalettePrimaryColor, Keys.selectedPrimaryColor)

fun accentColorFlow(context: Context): Flow<Int> =
    colorFlow(context, Keys.monetPaletteAccentColor, Keys.selectedAccentColor)

object ThemeCacheUpdateDelegate {

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private fun <T> observeSetting(context: Context, key: PrimitiveKey<T>, collector: FlowCollector<T>): Job =
        scope.launch { Setting(context)[key].flow.distinctUntilChanged().collect(collector) }

    private var job: Job? = null
    private fun observeThemeColor(context: Context) {
        job = scope.launch {
            observeSetting(context, Keys.selectedPrimaryColor) {
                ThemeSetting.updateCachedPrimaryColor(context)
            }
            observeSetting(context, Keys.monetPalettePrimaryColor) {
                ThemeSetting.updateCachedPrimaryColor(context)
            }
            observeSetting(context, Keys.selectedAccentColor) {
                ThemeSetting.updateCachedAccentColor(context)
            }
            observeSetting(context, Keys.monetPaletteAccentColor) {
                ThemeSetting.updateCachedAccentColor(context)
            }
            observeSetting(context, Keys.enableMonet) {
                ThemeSetting.updateCachedPrimaryColor(context)
                ThemeSetting.updateCachedAccentColor(context)
            }
        }
    }

    fun start(context: Context) {
        observeThemeColor(context.applicationContext)
    }

    fun stop() {
        job?.cancel(CancellationException("Force stop"))
    }

}