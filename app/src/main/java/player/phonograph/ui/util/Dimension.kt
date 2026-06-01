/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.util

import android.content.res.Resources

fun convertDpToPixel(dp: Float, resources: Resources): Float {
    val metrics = resources.displayMetrics
    return dp * metrics.density
}

fun convertPixelsToDp(px: Float, resources: Resources): Float {
    val metrics = resources.displayMetrics
    return px / metrics.density
}