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

    fun Deferred<Palette>.getColor(@ColorInt fallbackColor: Int, callBack: (Int) -> Unit) {
        coroutineScope.launch {
            val color = PhonographColorUtil.getColor(this@getColor.await(), fallbackColor)
            withContext(Dispatchers.Main) {
                callBack(color)
            }
        }
    }
}
