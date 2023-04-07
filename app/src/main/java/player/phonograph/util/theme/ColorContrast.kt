/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.theme

import mt.util.color.darkenColor
import mt.util.color.isColorLight
import mt.util.color.lightenColor
import androidx.annotation.ColorInt

/**
 * darken the color if light for once
 */
@ColorInt
fun requireDarkenColor(@ColorInt color: Int): Int =
    if (isColorLight(color)) darkenColor(color) else color

@ColorInt
fun shiftBackgroundColorForLightText(@ColorInt backgroundColor: Int): Int {
    var newColor = backgroundColor
    while (isColorLight(newColor)) newColor = darkenColor(newColor)
    return newColor
}

@ColorInt
fun shiftBackgroundColorForDarkText(@ColorInt backgroundColor: Int): Int {
    var newColor = backgroundColor
    while (!isColorLight(newColor)) newColor = lightenColor(newColor)
    return newColor
}