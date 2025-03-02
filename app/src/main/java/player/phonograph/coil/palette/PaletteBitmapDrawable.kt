/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil.palette

import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable

/**
 * [BitmapDrawable] with [Palette]
 */
class PaletteBitmapDrawable(
    resources: Resources,
    bitmap: Bitmap,
    val palette: Palette,
) : BitmapDrawable(resources, bitmap) {

    fun color(fallback: Int): Int {
        val swatch = with(palette) { vibrantSwatch ?: mutedSwatch }
        return swatch?.rgb ?: fallback
    }

    companion object {
        @JvmStatic
        fun from(resources: Resources, bitmap: Bitmap): PaletteBitmapDrawable {

            val builder = Palette
                .from(bitmap)
                .clearTargets()
                .addTarget(Target.VIBRANT)
                .addTarget(Target.MUTED)

            val palette = builder.generate()

            return PaletteBitmapDrawable(resources, bitmap, palette)
        }
    }
}