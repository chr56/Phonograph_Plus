/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs

fun Color.isColorLight(): Boolean = luminance() >= 0.5f

fun Color.getReverseColor(): Color {
    val r = colorSpace.getMaxValue(1) - red
    val g = colorSpace.getMaxValue(2) - green
    val b = colorSpace.getMaxValue(3) - blue
    return Color(r, g, b, alpha, colorSpace)
}

object ColorTools {
    fun isColorRelevant(a: Color, b: Color): Boolean {
        return (abs(a.luminance() - b.luminance()) <= 0.0625f) or (
            (abs(a.red - b.red) <= 0.08) and (abs(a.green - b.green) <= 0.08) and (abs(a.blue - b.blue) <= 0.08)
            )
    }
}
