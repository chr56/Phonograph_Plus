/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target

import coil.target.Target
import player.phonograph.coil.target.PaletteUtil.processPalette
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import android.graphics.drawable.Drawable
import android.view.View
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
                onStart = onStart,
                onError = onFail,
                onSuccess = { result: Drawable, palette: Deferred<Palette>? ->
                    processPalette(result, palette, fallbackColor, onResourceReady)
                }
            )
        } else {
            return PaletteDelegateTarget.create(
                onStart = onStart,
                onError = onFail,
                onSuccess = { result: Drawable, palette: Deferred<Palette>? ->
                    processPalette(result, palette, fallbackColor, onResourceReady)
                }
            )
        }
    }

    companion object {
        private val coroutineScope =
            CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName("PaletteGenerator"))
    }
}