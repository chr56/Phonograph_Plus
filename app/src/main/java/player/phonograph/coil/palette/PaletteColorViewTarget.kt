/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil.palette

import coil.target.ImageViewTarget
import android.graphics.drawable.Drawable
import android.widget.ImageView


class PaletteColorViewTarget(
    view: ImageView,
    val container: PaletteContainer,
    val defaultColor: Int,
    val enable: Boolean = true,
) : ImageViewTarget(view) {

    override fun onStart(placeholder: Drawable?) {
        super.onStart(placeholder)
        container.updatePaletteColor(defaultColor)
    }

    override fun onSuccess(result: Drawable) {
        super.onSuccess(result)
        val color = if (enable && result is PaletteBitmapDrawable) result.color(defaultColor) else defaultColor
        container.updatePaletteColor(color)
    }

    override fun onError(error: Drawable?) {
        super.onError(error)
        container.updatePaletteColor(defaultColor)
    }
}