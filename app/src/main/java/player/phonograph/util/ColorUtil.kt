/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util

import mt.util.color.darkenColor
import mt.util.color.isColorLight
import androidx.annotation.ColorInt

object ColorUtil {
    /**
     * darken the color if light for once
     */
    @ColorInt
    fun requireDarkenColor(@ColorInt color: Int): Int =
        if (isColorLight(color)) darkenColor(color) else color
}