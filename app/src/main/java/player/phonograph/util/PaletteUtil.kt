/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util

import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import android.graphics.Bitmap

object PaletteUtil {

    fun paletteColor(bitmap: Bitmap, @ColorInt fallbackColor: Int): Int {
        val builder = Palette
            .from(bitmap)
            .clearTargets()
            .addTarget(Target.VIBRANT)
            .addTarget(Target.MUTED)

        val palette = builder.generate()

        val swatch = with(palette) { vibrantSwatch ?: mutedSwatch }
        return swatch?.rgb ?: fallbackColor
    }

}
