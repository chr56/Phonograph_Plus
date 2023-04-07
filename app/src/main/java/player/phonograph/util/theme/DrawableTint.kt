/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.util.ImageUtil.drawableColorFilter
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeCompat
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

