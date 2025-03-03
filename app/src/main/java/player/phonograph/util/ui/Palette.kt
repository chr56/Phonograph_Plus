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

fun paletteColor(bitmap: Bitmap, @ColorInt fallbackColor: Int): Int {
    val palette = bitmap.generatePalette()
    val swatch = with(palette) { vibrantSwatch ?: mutedSwatch }
    return swatch?.rgb ?: fallbackColor
}