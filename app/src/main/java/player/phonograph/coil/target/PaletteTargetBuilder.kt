/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target

import coil.target.Target
import androidx.annotation.ColorInt
import android.graphics.drawable.Drawable
import android.view.View

class PaletteTargetBuilder {

    private var _defaultColorHasSet = false
    private var _defaultColor: Int = 0
    fun defaultColor(@ColorInt value: Int): PaletteTargetBuilder =
        this.apply {
            _defaultColorHasSet = true
            _defaultColor = value
        }

    private var _view: View? = null
    fun view(view: View): PaletteTargetBuilder =
        this.apply {
            _view = view
        }

    private var onResourceReady: (result: Drawable, paletteColor: Int) -> Unit = { _, _ -> }
    fun onResourceReady(block: (result: Drawable, paletteColor: Int) -> Unit): PaletteTargetBuilder =
        this.apply {
            onResourceReady = block
        }

    private var onFail: (error: Drawable?) -> Unit = {}
    fun onFail(block: (error: Drawable?) -> Unit): PaletteTargetBuilder =
        this.apply {
            onFail = block
        }

    private var onStart: (placeholder: Drawable?) -> Unit = {}
    fun onStart(block: (placeholder: Drawable?) -> Unit): PaletteTargetBuilder =
        this.apply {
            onStart = block
        }

    fun build(): Target {
        if (!_defaultColorHasSet) throw RuntimeException("No DefaultColor!")
        val fallbackColor = _defaultColor

        val view = _view
        if (view != null) {
            return ViewPaletteDelegateTarget.create(
                view,
                fallbackColor,
                onStart = onStart,
                onError = onFail,
                onSuccess = onResourceReady
            )
        } else {
            return PaletteDelegateTarget.create(
                fallbackColor,
                onStart = onStart,
                onError = onFail,
                onSuccess = onResourceReady
            )
        }
    }

}