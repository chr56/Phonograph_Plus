/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.settings

import lib.phonograph.misc.MonetColor
import player.phonograph.R
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object ThemeSetting {

    @CheckResult
    @StyleRes
    fun themeStyle(context: Context): Int =
        parseToStyleRes(Setting(context)[Keys.theme].data)

    @CheckResult
    @ColorInt
    fun primaryColor(context: Context): Int =
        if (!isPrimaryColorUpdated) updateCachedPrimaryColor(context) else cachedPrimaryColor

    @CheckResult
    @ColorInt
    fun accentColor(context: Context): Int =
        if (!isAccentColorUpdated) updateCachedAccentColor(context) else cachedAccentColor

    private var isPrimaryColorUpdated = false
    private var isAccentColorUpdated = false


    @ColorInt
    private var cachedPrimaryColor: Int = 0

    @ColorInt
    private var cachedAccentColor: Int = 0

    @ColorInt
    private fun updateCachedPrimaryColor(context: Context): Int {
        val setting = Setting(context)
        val primaryColor =
            if (SDK_INT >= VERSION_CODES.S && setting[Keys.enableMonet].data) {
                MonetColor.MonetColorPalette(
                    Setting(context)[Keys.monetPalettePrimaryColor].data
                ).color(context)
            } else {
                Setting(context)[Keys.selectedPrimaryColor].data
            }
        cachedPrimaryColor = primaryColor
        isPrimaryColorUpdated = true
        return primaryColor
    }

    @ColorInt
    private fun updateCachedAccentColor(context: Context): Int {
        val setting = Setting(context)
        val primaryColor =
            if (SDK_INT >= VERSION_CODES.S && setting[Keys.enableMonet].data) {
                MonetColor.MonetColorPalette(
                    Setting(context)[Keys.monetPaletteAccentColor].data
                ).color(context)
            } else {
                Setting(context)[Keys.selectedAccentColor].data
            }
        cachedAccentColor = primaryColor
        isAccentColorUpdated = true
        return primaryColor
    }

    /**
     * observe color changed
     * @param onChanged callback (primary color & accent color)
     */
    suspend fun observeColors(context: Context, onChanged: (Int, Int) -> Unit) {
        val setting = Setting(context.applicationContext)
        val mode = setting[Keys.enableMonet]
        val flowSelectedPrimaryColor =
            setting[Keys.selectedPrimaryColor].flow
        val flowSelectedAccentColor =
            setting[Keys.selectedAccentColor].flow
        val flowMonetPalettePrimaryColor =
            setting[Keys.monetPalettePrimaryColor].flow
        val flowMonetPaletteAccentColor =
            setting[Keys.monetPaletteAccentColor].flow
        withContext(Dispatchers.IO) {
            mode.flow.collect {
                delay(250)
                onChanged(updateCachedPrimaryColor(context), updateCachedAccentColor(context))
            }
            flowSelectedPrimaryColor.collect {
                delay(100)
                if (mode.data) {
                    onChanged(updateCachedPrimaryColor(context), cachedAccentColor)
                }
            }
            flowSelectedAccentColor.collect {
                delay(100)
                if (mode.data) {
                    onChanged(cachedPrimaryColor, updateCachedAccentColor(context))
                }
            }
            flowMonetPalettePrimaryColor.collect {
                delay(100)
                if (mode.data) {
                    onChanged(updateCachedPrimaryColor(context), cachedAccentColor)
                }
            }
            flowMonetPaletteAccentColor.collect {
                delay(100)
                if (mode.data) {
                    onChanged(cachedPrimaryColor, updateCachedAccentColor(context))
                }
            }
        }
    }

    @JvmName("Context_PrimaryColor")
    @CheckResult
    @ColorInt
    fun Context.primaryColor(): Int = primaryColor(this)


    @JvmName("Context_AccentColor")
    @CheckResult
    @ColorInt
    fun Context.accentColor(): Int = accentColor(this)

    @StyleRes
    private fun parseToStyleRes(@GeneralTheme theme: String): Int =
        when (theme) {
            THEME_AUTO  -> R.style.Theme_Phonograph_Auto
            THEME_DARK  -> R.style.Theme_Phonograph_Dark
            THEME_BLACK -> R.style.Theme_Phonograph_Black
            THEME_LIGHT -> R.style.Theme_Phonograph_Light
            else        -> R.style.Theme_Phonograph_Auto
        }
}