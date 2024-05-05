/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.settings

import lib.phonograph.misc.MonetColor
import util.theme.color.shiftColor
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import android.content.Context
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object ThemeSetting {

    @CheckResult
    @ColorInt
    fun primaryColor(context: Context): Int =
        if (cachedPrimaryColor <= 0) updateCachedPrimaryColor(context) else cachedPrimaryColor

    @CheckResult
    @ColorInt
    fun primaryColorDark(context: Context): Int =
        shiftColor(primaryColor(context), 0.9f)

    @CheckResult
    @ColorInt
    fun accentColor(context: Context): Int =
        if (cachedAccentColor <= 0) updateCachedAccentColor(context) else cachedAccentColor

    @CheckResult
    @ColorInt
    fun navigationBarColor(context: Context): Int =
        if (Setting(context)[Keys.coloredNavigationBar].data) primaryColor(context) else Color.BLACK

    @CheckResult
    @ColorInt
    fun statusBarColor(context: Context): Int =
        if (Setting(context)[Keys.coloredStatusbar].data) primaryColorDark(context) else Color.BLACK


    @ColorInt
    private var cachedPrimaryColor: Int = -1

    @ColorInt
    private var cachedAccentColor: Int = -1

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

}