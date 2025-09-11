/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.util.ui

import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import android.graphics.Bitmap

fun Bitmap.generatePalette(): Palette {
    val builder = Palette
        .from(this)
        .clearTargets()
        .addTarget(Target.VIBRANT)
        .addTarget(Target.MUTED)

    val palette = builder.generate()
    return palette
}

/**
 * Extracts a Palette color from a [Bitmap].
 * @param bitmap The [Bitmap] to extract the color from. Can be null.
 * @param fallbackColor The color to return if a suitable palette color is not available.
 * @param recycle If true, the input [Bitmap] will be recycled after color extraction. Defaults to false.
 * @return The extracted color (or the [fallbackColor]).
 */
@ColorInt
fun paletteColor(bitmap: Bitmap?, @ColorInt fallbackColor: Int, recycle: Boolean = false): Int {
    if (bitmap == null) return fallbackColor
    val palette = bitmap.generatePalette()
    val swatch = with(palette) { vibrantSwatch ?: mutedSwatch }
    val color = swatch?.rgb ?: fallbackColor
    if (recycle) bitmap.recycle()
    return color
}