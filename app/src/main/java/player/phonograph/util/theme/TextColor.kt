/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.R
import util.theme.color.isColorLight
import androidx.annotation.ColorInt
import android.content.Context

@ColorInt
fun textColorPrimary(context: Context): Int =
    context.resolveThemeColor(
        attr = android.R.attr.textColorPrimary,
        fallbackColorRes = R.color.default_text_color_primary,
    )

@ColorInt
fun textColorSecondary(context: Context): Int =
    context.resolveThemeColor(
        attr = android.R.attr.textColorSecondary,
        fallbackColorRes = R.color.default_text_color_secondary,
    )

@ColorInt
fun textColorOn(context: Context, @ColorInt backgroundColor: Int): Int =
    defaultTextColor(context, onDarkBackground = !isColorLight(backgroundColor))

@ColorInt
fun secondaryTextColorOn(context: Context, @ColorInt backgroundColor: Int): Int =
    secondaryTextColor(context, onDarkBackground = !isColorLight(backgroundColor))

@ColorInt
fun defaultTextColor(context: Context, onDarkBackground: Boolean): Int =
    context.getColor(
        if (onDarkBackground) R.color.primary_text_dark else R.color.primary_text_light
    )

@ColorInt
fun secondaryTextColor(context: Context, onDarkBackground: Boolean): Int =
    context.getColor(
        if (onDarkBackground) R.color.secondary_text_dark else R.color.secondary_text_light
    )