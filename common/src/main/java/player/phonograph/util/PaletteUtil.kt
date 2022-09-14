/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*

object PaletteUtil {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun Bitmap.toPaletteAsync(): Deferred<Palette> =
        coroutineScope.async {
            Palette.from(this@toPaletteAsync).generate()
        }

    suspend fun Deferred<Palette>.getColor(@ColorInt fallbackColor: Int): Int =
        try {
            withTimeout(1200) {
                val palette = this@getColor.await()
                palette.getColor(fallbackColor)
            }
        } catch (e: TimeoutCancellationException) {
            fallbackColor
        }

    @JvmStatic
    @ColorInt
    fun Palette?.getColor(fallback: Int): Int {
        if (this != null) {
            when {
                vibrantSwatch != null -> return vibrantSwatch!!.rgb
                mutedSwatch != null -> return mutedSwatch!!.rgb
                darkVibrantSwatch != null -> return darkVibrantSwatch!!.rgb
                darkMutedSwatch != null -> return darkMutedSwatch!!.rgb
                lightVibrantSwatch != null -> return lightVibrantSwatch!!.rgb
                lightMutedSwatch != null -> return lightMutedSwatch!!.rgb
                swatches.isNotEmpty() -> return java.util.Collections.max(swatches, SwatchComparator.instance).rgb
            }
        }
        return fallback
    }

    private class SwatchComparator : Comparator<Palette.Swatch> {
        override fun compare(lhs: Palette.Swatch, rhs: Palette.Swatch): Int = lhs.population - rhs.population

        companion object {
            private var mInstance: SwatchComparator? = null

            val instance: SwatchComparator?
                get() {
                    if (mInstance == null) mInstance = SwatchComparator()
                    return mInstance
                }
        }
    }

    @JvmStatic
    fun generatePalette(bitmap: Bitmap): Palette = Palette.from(bitmap).generate()

}