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

class PaletteDelegateTarget internal constructor(private val callbacks: Callbacks) : Target {

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

    interface Callbacks {
        fun onStart(placeholder: Drawable?) {}

        fun onError(error: Drawable?) {}

        fun onSuccess(result: Drawable, palette: Deferred<Palette>?) {}
    }
}

internal fun createBasePaletteTarget(t: PaletteDelegateTarget.Callbacks) = PaletteDelegateTarget(t)
internal inline fun createBasePaletteTarget(
    crossinline onStart: (placeholder: Drawable?) -> Unit = {},
    crossinline onError: (error: Drawable?) -> Unit = {},
    crossinline onSuccess: (result: Drawable, palette: Deferred<Palette>?) -> Unit = { _, _ -> },
) = PaletteDelegateTarget(object : PaletteDelegateTarget.Callbacks {
    override fun onStart(placeholder: Drawable?) = onStart(placeholder)
    override fun onError(error: Drawable?) = onError(error)
    override fun onSuccess(result: Drawable, palette: Deferred<Palette>?) =
        onSuccess(result, palette)
})
