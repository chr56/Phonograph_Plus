/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.target

import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout

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


    @ColorInt
    fun Palette.getColor(fallback: Int): Int {
        val swatchColor =
            vibrantSwatch
                ?: mutedSwatch
                ?: lightVibrantSwatch
                ?: lightMutedSwatch
                ?: darkVibrantSwatch
                ?: darkMutedSwatch
        return swatchColor?.rgb
            ?: if (swatches.isNotEmpty()) swatches.maxWith(SwatchComparator.instance!!).rgb else fallback
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
}
