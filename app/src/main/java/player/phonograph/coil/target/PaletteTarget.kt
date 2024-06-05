/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.palette.graphics.Palette
import coil.target.Target
import coil.target.ViewTarget
import kotlinx.coroutines.Deferred
import player.phonograph.coil.target.PaletteUtil.toPaletteAsync
import android.view.View

interface PaletteTarget {
    fun onStart(placeholder: Drawable?) {}

    fun onError(error: Drawable?) {}

    fun onSuccess(result: Drawable, palette: Deferred<Palette>?) {}
}

class PaletteDelegateTarget(private val delegate: PaletteTarget) : Target {

    override fun onStart(placeholder: Drawable?) {
        delegate.onStart(placeholder)
    }

    override fun onSuccess(result: Drawable) {
        if (result is BitmapDrawable) {
            delegate.onSuccess(result, result.bitmap.toPaletteAsync())
        } else {
            Log.w("PaletteTarget", "Not A Bitmap drawable: $result")
            delegate.onSuccess(result, null)
        }
    }

    override fun onError(error: Drawable?) {
        delegate.onError(error)
    }

    companion object {
        internal inline fun create(
            crossinline onStart: (placeholder: Drawable?) -> Unit = {},
            crossinline onError: (error: Drawable?) -> Unit = {},
            crossinline onSuccess: (result: Drawable, palette: Deferred<Palette>?) -> Unit = { _, _ -> },
        ) = PaletteDelegateTarget(object : PaletteTarget {
            override fun onStart(placeholder: Drawable?) = onStart(placeholder)
            override fun onError(error: Drawable?) = onError(error)
            override fun onSuccess(result: Drawable, palette: Deferred<Palette>?) =
                onSuccess(result, palette)
        })
    }
}

class ViewPaletteDelegateTarget<T : View>(private val delegate: PaletteTarget, override val view: T) : ViewTarget<T> {

    override fun onStart(placeholder: Drawable?) {
        delegate.onStart(placeholder)
    }

    override fun onSuccess(result: Drawable) {
        if (result is BitmapDrawable) {
            delegate.onSuccess(result, result.bitmap.toPaletteAsync())
        } else {
            Log.w("PaletteTarget", "Not A Bitmap drawable: $result")
            delegate.onSuccess(result, null)
        }
    }

    override fun onError(error: Drawable?) {
        delegate.onError(error)
    }

    companion object {
        inline fun <V : View> create(
            view: V,
            crossinline onStart: (placeholder: Drawable?) -> Unit = {},
            crossinline onError: (error: Drawable?) -> Unit = {},
            crossinline onSuccess: (result: Drawable, palette: Deferred<Palette>?) -> Unit = { _, _ -> },
        ) = ViewPaletteDelegateTarget(object : PaletteTarget {
            override fun onStart(placeholder: Drawable?) = onStart(placeholder)
            override fun onError(error: Drawable?) = onError(error)
            override fun onSuccess(result: Drawable, palette: Deferred<Palette>?) =
                onSuccess(result, palette)
        }, view)
    }
}