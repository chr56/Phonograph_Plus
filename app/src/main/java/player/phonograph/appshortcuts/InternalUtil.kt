/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.appshortcuts

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import mt.util.drawable.createTintedDrawable
import player.phonograph.util.ImageUtil

fun getTintedVectorDrawable(
    res: Resources,
    @DrawableRes resId: Int,
    theme: Resources.Theme?,
    @ColorInt color: Int
): Drawable? =
    createTintedDrawable(ImageUtil.getVectorDrawable(res, resId, theme), color)

fun getTintedVectorDrawable(context: Context, @DrawableRes id: Int, @ColorInt color: Int): Drawable {
    return createTintedDrawable(
        ImageUtil.getVectorDrawable(context.resources, id, context.theme),
        color
    )!!
}