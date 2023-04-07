/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.palette.graphics.Palette
import coil.target.Target
import kotlinx.coroutines.Deferred
import player.phonograph.coil.target.PaletteUtil.toPaletteAsync

class BasePaletteTarget internal constructor(private val callbacks: Target) : Target {

    override fun onStart(placeholder: Drawable?) {
        callbacks.onStart(placeholder)
    }

    override fun onSuccess(result: Drawable) {
        if (result is BitmapDrawable) {
            callbacks.onSuccess(result, result.bitmap.toPaletteAsync())
        } else {
            Log.w("PaletteTarget", "Not A Bitmap drawable: $result")
            callbacks.onSuccess(result, null)
        }
    }

    override fun onError(error: Drawable?) {
        callbacks.onError(error)
    }

    interface Target {
        fun onStart(placeholder: Drawable?) {}

        fun onError(error: Drawable?) {}

        fun onSuccess(result: Drawable, palette: Deferred<Palette>?) {}
    }
}

internal fun createBasePaletteTarget(t: BasePaletteTarget.Target) = BasePaletteTarget(t)
internal inline fun createBasePaletteTarget(
    crossinline onStart: (placeholder: Drawable?) -> Unit = {},
    crossinline onError: (error: Drawable?) -> Unit = {},
    crossinline onSuccess: (result: Drawable, palette: Deferred<Palette>?) -> Unit = { _, _ -> },
) = BasePaletteTarget(object : BasePaletteTarget.Target {
    override fun onStart(placeholder: Drawable?) = onStart(placeholder)
    override fun onError(error: Drawable?) = onError(error)
    override fun onSuccess(result: Drawable, palette: Deferred<Palette>?) =
        onSuccess(result, palette)
})
