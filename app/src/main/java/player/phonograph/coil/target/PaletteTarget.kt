/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target

import coil.target.Target
import coil.target.ViewTarget
import player.phonograph.coil.target.PaletteUtil.paletteColor
import player.phonograph.util.debug
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface PaletteTarget {
    fun onStart(placeholder: Drawable?) {}

    fun onError(error: Drawable?) {}

    fun onSuccess(result: Drawable, paletteColor: Int) {}

}

class PaletteDelegateTarget(
    private val delegate: PaletteTarget,
    private val defaultColor: Int,
) : Target {

    override fun onStart(placeholder: Drawable?) {
        delegate.onStart(placeholder)
    }

    override fun onSuccess(result: Drawable) {
        coroutineScope.launch(Dispatchers.IO) {
            val bitmap = (result as? BitmapDrawable)?.bitmap
            val paletteColor = if (bitmap != null) {
                paletteColor(bitmap, defaultColor)
            } else {
                debug {
                    Log.w("PaletteTarget", "Not A Bitmap drawable: $result")
                }
                defaultColor
            }
            coroutineScope.launch(Dispatchers.Main.immediate) {
                delegate.onSuccess(result, paletteColor)
            }
        }
    }

    override fun onError(error: Drawable?) {
        delegate.onError(error)
    }

    companion object {
        internal inline fun create(
            defaultColor: Int,
            crossinline onStart: (placeholder: Drawable?) -> Unit = {},
            crossinline onError: (error: Drawable?) -> Unit = {},
            crossinline onSuccess: (result: Drawable, paletteColor: Int) -> Unit = { _, _ -> },
        ) = PaletteDelegateTarget(object : PaletteTarget {
            override fun onStart(placeholder: Drawable?) = onStart(placeholder)
            override fun onError(error: Drawable?) = onError(error)
            override fun onSuccess(result: Drawable, paletteColor: Int) =
                onSuccess(result, paletteColor)
        }, defaultColor)
    }
}

class ViewPaletteDelegateTarget<T : View>(
    private val delegate: PaletteTarget,
    private val defaultColor: Int,
    override val view: T,
) : ViewTarget<T> {

    override fun onStart(placeholder: Drawable?) {
        delegate.onStart(placeholder)
    }

    override fun onSuccess(result: Drawable) {
        coroutineScope.launch(Dispatchers.IO) {
            val bitmap = (result as? BitmapDrawable)?.bitmap
            val paletteColor = if (bitmap != null) {
                paletteColor(bitmap, defaultColor)
            } else {
                debug {
                    Log.w("PaletteTarget", "Not A Bitmap drawable: $result")
                }
                defaultColor
            }
            coroutineScope.launch(Dispatchers.Main.immediate) {
                delegate.onSuccess(result, paletteColor)
            }
        }
    }

    override fun onError(error: Drawable?) {
        delegate.onError(error)
    }

    companion object {
        inline fun <V : View> create(
            view: V,
            defaultColor: Int,
            crossinline onStart: (placeholder: Drawable?) -> Unit = {},
            crossinline onError: (error: Drawable?) -> Unit = {},
            crossinline onSuccess: (result: Drawable, paletteColor: Int) -> Unit = { _, _ -> },
        ) = ViewPaletteDelegateTarget(object : PaletteTarget {
            override fun onStart(placeholder: Drawable?) = onStart(placeholder)
            override fun onError(error: Drawable?) = onError(error)
            override fun onSuccess(result: Drawable, paletteColor: Int) =
                onSuccess(result, paletteColor)
        }, defaultColor, view)
    }
}

private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())