/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.view.WindowManager

fun Context.getScreenSize(): Point {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val size: Point =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.run { Point(width(), height()) }
        } else {
            Point().also { windowManager.defaultDisplay.getSize(it) }
        }
    return size
}

fun isTablet(resources: Resources): Boolean {
    return resources.configuration.smallestScreenWidthDp >= 600
}

fun isLandscape(resources: Resources): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}