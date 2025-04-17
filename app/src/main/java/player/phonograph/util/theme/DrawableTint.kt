/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.theme

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.View


fun Drawable.withColorFilter(
    @ColorInt color: Int,
    mode: BlendModeCompat = BlendModeCompat.SRC_IN,
): Drawable = apply { colorFilter = blendColorFilter(color, mode) }

fun Resources.getTintedDrawable(
    @DrawableRes id: Int,
    @ColorInt color: Int,
    theme: Resources.Theme? = null,
    mode: BlendModeCompat = BlendModeCompat.SRC_IN,
): Drawable? = ResourcesCompat.getDrawable(this, id, theme)?.withColorFilter(color, mode)

fun Context.getTintedDrawable(
    @DrawableRes id: Int,
    @ColorInt color: Int,
    mode: BlendModeCompat = BlendModeCompat.SRC_IN,
): Drawable? = resources.getTintedDrawable(id, color, theme, mode)

fun Fragment.getTintedDrawable(
    @DrawableRes id: Int,
    @ColorInt color: Int,
    mode: BlendModeCompat = BlendModeCompat.SRC_IN,
): Drawable? = resources.getTintedDrawable(id, color, null, mode)

fun View.getTintedDrawable(
    @DrawableRes id: Int,
    @ColorInt color: Int,
    mode: BlendModeCompat = BlendModeCompat.SRC_IN,
): Drawable? = resources.getTintedDrawable(id, color, context.theme, mode)


fun blendColorFilter(color: Int, mode: BlendModeCompat) =
    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, mode)