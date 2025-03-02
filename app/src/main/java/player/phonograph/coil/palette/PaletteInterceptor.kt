/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil.palette

import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.Parameters
import coil.request.SuccessResult
import android.graphics.drawable.BitmapDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaletteInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val palette = chain.request.parameters.value<Boolean>(PARAMETERS_KEY_PALETTE) == true

        if (palette == true) {
            val result = chain.proceed(chain.request)
            val drawable = result.drawable
            if (drawable is BitmapDrawable && result is SuccessResult) {
                val paletteBitmapDrawable = withContext(Dispatchers.Default) {
                    PaletteBitmapDrawable.from(chain.request.context.resources, drawable.bitmap)
                }
                return result.copy(drawable = paletteBitmapDrawable)
            }
        }
        return chain.proceed(chain.request)
    }

    companion object {

        const val PARAMETERS_KEY_PALETTE = "palette"
        val PARAMETERS_PALETTE = Parameters.Builder().set(PARAMETERS_KEY_PALETTE, true).build()
    }
}