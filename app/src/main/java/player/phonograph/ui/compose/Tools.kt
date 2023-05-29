/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.compose

import mt.util.color.primaryTextColor
import player.phonograph.mechanism.setting.StyleConfig
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.graphics.Color.RGBToHSV
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun composeDarkTheme(): Boolean {

    // val systemUiMode =
    //     (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    return StyleConfig.isNightMode(LocalContext.current)
}

fun Color.isColorLight(): Boolean = luminance() >= 0.5f

fun Color.getReverseColor(): Color {
    val r = colorSpace.getMaxValue(1) - red
    val g = colorSpace.getMaxValue(2) - green
    val b = colorSpace.getMaxValue(3) - blue
    return Color(r, g, b, alpha, colorSpace)
}

fun textColorOn(context: Context, color: Color): Color =
    Color(
        context.primaryTextColor(color.toArgb())
    )

object ColorTools {
    fun isColorRelevant(a: Color, b: Color): Boolean {
        return (abs(a.luminance() - b.luminance()) <= 0.0625f) or (
            (abs(a.red - b.red) <= 0.08) and (abs(a.green - b.green) <= 0.08) and (abs(a.blue - b.blue) <= 0.08)
            )
    }

    inline fun makeSureContrastWith(backgroundColor: Color, block: () -> Color): Color {
        val goingDarker = backgroundColor.isColorLight()
        var newColor = block()
        while (isColorRelevant(newColor, backgroundColor)) {
            newColor = if (goingDarker) newColor.darker() else newColor.lighter()
        }
        return newColor
    }
}

private fun Color.hsvShift(by: Float): Color {
    val hsv = floatArrayOf(0f, 0f, 0f)
    RGBToHSV(red.roundToInt(), green.roundToInt(), blue.roundToInt(), hsv)
    return Color.hsv(hsv[0], hsv[1], hsv[2] * by)
}

fun Color.darker(): Color = hsvShift(0.8f)

fun Color.lighter(): Color = hsvShift(1.25f)
