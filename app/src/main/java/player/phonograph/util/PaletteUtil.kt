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
                PhonographColorUtil.getColor(palette, fallbackColor)
            }
        } catch (e: TimeoutCancellationException) {
            fallbackColor
        }
}
