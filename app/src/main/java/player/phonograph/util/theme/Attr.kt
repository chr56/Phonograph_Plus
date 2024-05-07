/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.R
import androidx.annotation.AttrRes
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import android.content.Context


@CheckResult
@ColorInt
fun themeFooterColor(context: Context) =
    context.resolveColor(
        R.attr.defaultFooterColor,
        context.getColor(R.color.background_footer_lightdark)
    )

@CheckResult
@ColorInt
fun themeIconColor(context: Context) =
    context.resolveColor(
        R.attr.iconColor,
        context.getColor(R.color.icon_lightdark)
    )

@CheckResult
@ColorInt
fun themeDividerColor(context: Context) =
    context.resolveColor(
        R.attr.dividerColor,
        context.getColor(R.color.divider_lightdark)
    )


@CheckResult
@ColorInt
fun themeCardBackgroundColor(context: Context) =
    context.resolveColor(
        com.google.android.material.R.attr.cardBackgroundColor,
        context.getColor(R.color.background_medium_lightblack)
    )

@CheckResult
@ColorInt
fun themeFloatingBackgroundColor(context: Context) =
    context.resolveColor(
        com.google.android.material.R.attr.colorBackgroundFloating,
        context.getColor(R.color.background_medium_lightblack)
    )

@ColorInt
fun Context.resolveColor(@AttrRes attr: Int, @ColorInt fallbackColor: Int): Int {
    val a = theme.obtainStyledAttributes(intArrayOf(attr))
    return try {
        a.getColor(0, fallbackColor)
    } finally {
        a.recycle()
    }
}