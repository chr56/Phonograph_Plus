package player.phonograph.util

import mt.pref.ThemeColor
import mt.util.color.darkenColor
import mt.util.color.isColorLight
import mt.util.color.lightenColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.settings.Setting
import player.phonograph.util.preferences.StyleConfig
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Build
import java.util.Collections


/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object PhonographColorUtil {
    /**
     * darken the color if light for once
     */
    @ColorInt
    fun requireDarkenColor(@ColorInt color: Int): Int =
        if (isColorLight(color)) darkenColor(color) else color

    @JvmStatic
    val Resources.nightMode: Boolean
        get() = nightMode(App.instance)

    fun Resources.nightMode(context: Context): Boolean =
        when (StyleConfig.generalTheme(context)) {
            R.style.Theme_Phonograph_Black -> true
            R.style.Theme_Phonograph_Dark  -> true
            R.style.Theme_Phonograph_Light -> false
            R.style.Theme_Phonograph_Auto  ->
                when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                    Configuration.UI_MODE_NIGHT_YES -> true
                    Configuration.UI_MODE_NIGHT_NO  -> false
                    else                            -> false
                }
            else                           -> false
        }

    fun Context.backgroundColorByTheme(): Int = resources.getColor(
        when (StyleConfig.generalTheme(this)) {
            R.style.Theme_Phonograph_Auto  -> R.color.cardBackgroundColor
            R.style.Theme_Phonograph_Light -> R.color.md_white_1000
            R.style.Theme_Phonograph_Black -> R.color.md_black_1000
            R.style.Theme_Phonograph_Dark  -> R.color.md_grey_800
            else                           -> R.color.md_grey_700
        },
        theme
    )

    /**
     * adjust color settings from Dynamic Color of Material You if available
     */
    fun applyMonet(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Setting.instance.enableMonet) {
            ThemeColor.editTheme(context)
                .primaryColor(context.getColor(android.R.color.system_accent1_300))
                .accentColor(context.getColor(android.R.color.system_accent1_600))
                .commit()
        }
    }

    @JvmStatic
    @ColorInt
    fun shiftBackgroundColorForLightText(@ColorInt backgroundColor: Int): Int {
        var newColor = backgroundColor
        while (isColorLight(newColor)) newColor = darkenColor(newColor)
        return newColor
    }

    @ColorInt
    fun shiftBackgroundColorForDarkText(@ColorInt backgroundColor: Int): Int {
        var newColor = backgroundColor
        while (!isColorLight(newColor)) newColor = lightenColor(newColor)
        return newColor
    }

    @JvmStatic
    fun generatePalette(bitmap: Bitmap): Palette = Palette.from(bitmap).generate()

    @JvmStatic
    @ColorInt
    fun getColor(palette: Palette?, fallback: Int): Int {
        if (palette != null) {
            when {
                palette.vibrantSwatch != null      -> return palette.vibrantSwatch!!.rgb
                palette.mutedSwatch != null        -> return palette.mutedSwatch!!.rgb
                palette.darkVibrantSwatch != null  -> return palette.darkVibrantSwatch!!.rgb
                palette.darkMutedSwatch != null    -> return palette.darkMutedSwatch!!.rgb
                palette.lightVibrantSwatch != null -> return palette.lightVibrantSwatch!!.rgb
                palette.lightMutedSwatch != null   -> return palette.lightMutedSwatch!!.rgb
                palette.swatches.isNotEmpty()      ->
                    return Collections.max(palette.swatches, SwatchComparator.instance).rgb
            }
        }
        return fallback
    }

    private class SwatchComparator : Comparator<Swatch> {
        override fun compare(lhs: Swatch, rhs: Swatch): Int = lhs.population - rhs.population

        companion object {
            private var mInstance: SwatchComparator? = null

            val instance: SwatchComparator?
                get() {
                    if (mInstance == null) mInstance = SwatchComparator()
                    return mInstance
                }
        }
    }
}
