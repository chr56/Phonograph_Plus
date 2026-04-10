/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.R
import androidx.annotation.AttrRes
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StyleableRes
import android.content.Context
import android.util.TypedValue


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
fun Context.resolveThemeColor(
    @AttrRes attr: Int,
    @ColorRes fallbackColorRes: Int,
    fallbackColor: (() -> Int)? = null,
): Int {
    val value = resolve(attr, true)
    return if (value != null) {
        if (value.resourceId != 0) {
            getColor(value.resourceId)
        } else {
            value.data
        }
    } else {
        fallbackColor?.invoke() ?: getColor(fallbackColorRes)
    }
}

fun Context.resolveThemeColors(
    @StyleableRes attrs: IntArray,
    @ColorRes fallbackColorRes: Int,
    fallbackColor: (() -> Int)? = null,
): IntArray {
    val values = theme.obtainStyledAttributes(attrs)
    val default: Int by lazy { (fallbackColor?.invoke() ?: getColor(fallbackColorRes)) }
    return try {
        IntArray(values.length()) { i ->
            values.getColor(i, default)
        }
    } finally {
        values.recycle()
    }
}

@ColorInt
fun Context.resolveColor(@AttrRes attr: Int, @ColorInt fallbackColor: Int): Int {
    val value = resolve(attr, true)
    return if (value != null) {
        if (value.resourceId != 0) {
            getColor(value.resourceId)
        } else {
            value.data
        }
    } else {
        fallbackColor
    }
}

fun Context.resolveColors(@StyleableRes attrs: IntArray, @ColorInt fallbackColor: Int): IntArray {
    val values = theme.obtainStyledAttributes(attrs)
    return try {
        IntArray(values.length()) { i ->
            values.getColor(i, fallbackColor)
        }
    } finally {
        values.recycle()
    }
}


fun Context.resolve(attr: Int, deep: Boolean): TypedValue? {
    val typedValue = TypedValue()
    return if (theme.resolveAttribute(attr, typedValue, deep)) typedValue else null
}