/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.palette.graphics.Palette
import coil.target.Target
import kotlinx.coroutines.Deferred
import player.phonograph.coil.PaletteUtil.toPaletteAsync

abstract class PaletteTarget : Target {

    var palette: Deferred<Palette>? = null
    var bitmap: Bitmap? = null

    override fun onStart(placeholder: Drawable?) {
    }

    override fun onSuccess(result: Drawable) {
        if (result is BitmapDrawable) {
            bitmap = result.bitmap
            palette = result.bitmap.toPaletteAsync()
        } else {
            Log.v("Palette", "Not Bitmap drawable: $result")
        }
    }

    override fun onError(error: Drawable?) {
    }
}
