/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil.palette

import coil.target.Target
import android.graphics.drawable.Drawable


class PaletteColorTarget(
    val defaultColor: Int,
    val start: (placeholder: Drawable?, defaultColor: Int) -> Unit = { _, _ -> },
    val success: (result: Drawable, paletteColor: Int) -> Unit = { _, _ -> },
    val error: (error: Drawable?, defaultColor: Int) -> Unit = { _, _ -> },
    val enable: Boolean = true,
) : Target {

    override fun onStart(placeholder: Drawable?) {
        start(placeholder, defaultColor)
    }

    override fun onSuccess(result: Drawable) {
        success(result, if (enable && result is PaletteBitmapDrawable) result.color(defaultColor) else defaultColor)
    }

    override fun onError(error: Drawable?) {
        error(error, defaultColor)
    }
}