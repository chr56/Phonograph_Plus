/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.target

import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Target
import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaletteUtil {

    private val coroutineName = CoroutineName("PaletteGenerator")

    suspend fun paletteColor(bitmap: Bitmap, @ColorInt fallbackColor: Int): Int {
        val builder = Palette
            .from(bitmap)
            .clearTargets()
            .addTarget(Target.VIBRANT)
            .addTarget(Target.MUTED)

        val palette = withContext(Dispatchers.Default + coroutineName) {
            builder.generate()
        }

        val swatch = with(palette) {
            vibrantSwatch ?: mutedSwatch
        }
        return swatch?.rgb ?: fallbackColor
    }

}
