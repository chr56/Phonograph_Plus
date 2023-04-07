/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.theme

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.content.Context
import android.graphics.drawable.Drawable


fun Context.getTintedDrawable(
    @DrawableRes id: Int,
    @ColorInt color: Int,
    mode: BlendModeCompat = BlendModeCompat.SRC_IN,
): Drawable? {
    return ResourcesCompat.getDrawable(this.resources, id, theme)?.also {
        it.colorFilter = drawableColorFilter(color, mode)
    }
}


fun Drawable.makeTintedDrawable(
    @ColorInt color: Int,
    mode: BlendModeCompat = BlendModeCompat.SRC_IN,
): Drawable {
    return DrawableCompat.wrap(this).also { newDrawable ->
        newDrawable.colorFilter = drawableColorFilter(color, mode)
    }
}

fun Context.createTintedDrawable(
    @DrawableRes id: Int,
    @ColorInt color: Int,
    mode: BlendModeCompat = BlendModeCompat.SRC_IN,
): Drawable? {
    val drawable = ResourcesCompat.getDrawable(resources, id, theme) ?: return null
    return drawable.makeTintedDrawable(color, mode)
}


fun drawableColorFilter(color: Int, mode: BlendModeCompat) =
    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, mode)