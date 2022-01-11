package player.phonograph.util

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import chr_56.MDthemer.util.ColorUtil
import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object PhonographColorUtil {

    @JvmStatic
    @ColorInt
    fun shiftBackgroundColorForLightText(@ColorInt backgroundColor: Int): Int {
        var newColor = backgroundColor
        while (ColorUtil.isColorLight(newColor)) newColor = ColorUtil.darkenColor(newColor)
        return newColor
    }

    @ColorInt
    fun shiftBackgroundColorForDarkText(@ColorInt backgroundColor: Int): Int {
        var newColor = backgroundColor
        while (!ColorUtil.isColorLight(newColor)) newColor = ColorUtil.lightenColor(newColor)
        return newColor
    }

    @JvmStatic
    fun generatePalette(bitmap: Bitmap): Palette = Palette.from(bitmap).generate()

    @JvmStatic
    @ColorInt
    fun getColor(palette: Palette?, fallback: Int): Int {
        if (palette != null) {
            when {
                palette.vibrantSwatch != null -> return palette.vibrantSwatch!!.rgb
                palette.mutedSwatch != null -> return palette.mutedSwatch!!.rgb
                palette.darkVibrantSwatch != null -> return palette.darkVibrantSwatch!!.rgb
                palette.darkMutedSwatch != null -> return palette.darkMutedSwatch!!.rgb
                palette.lightVibrantSwatch != null -> return palette.lightVibrantSwatch!!.rgb
                palette.lightMutedSwatch != null -> return palette.lightMutedSwatch!!.rgb
                palette.swatches.isNotEmpty() -> return Collections.max(palette.swatches, SwatchComparator.instance).rgb
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
