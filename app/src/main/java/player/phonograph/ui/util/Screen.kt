/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.util

import androidx.annotation.IntDef
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import kotlin.math.abs

fun Context.getScreenSize(): Point {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val size: Point =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.run { Point(width(), height()) }
        } else {
            @Suppress("DEPRECATION")
            Point().also { windowManager.defaultDisplay.getSize(it) }
        }
    return size
}

fun isOrientationLandscape(resources: Resources): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

fun isTablet(resources: Resources): Boolean = resources.configuration.smallestScreenWidthDp >= 600


const val SCREEN_CATEGORY_PORTRAIT = 1
const val SCREEN_CATEGORY_LANDSCAPE = 2
const val SCREEN_CATEGORY_SQUARE = 5
const val SCREEN_CATEGORY_MINI = 15

@IntDef(SCREEN_CATEGORY_PORTRAIT, SCREEN_CATEGORY_LANDSCAPE, SCREEN_CATEGORY_SQUARE, SCREEN_CATEGORY_MINI)
@Retention(AnnotationRetention.SOURCE)
annotation class ScreenCategory

@ScreenCategory
fun detectScreenCategory(resources: Resources): Int {
    val width = resources.configuration.screenWidthDp
    val height = resources.configuration.screenHeightDp

    if (width + height <= 240) return SCREEN_CATEGORY_MINI

    if (abs(height - width) / (width + 0.0625f) <= 0.125f) return SCREEN_CATEGORY_SQUARE

    return if (width >= height) {
        SCREEN_CATEGORY_LANDSCAPE
    } else {
        SCREEN_CATEGORY_PORTRAIT
    }
}

fun isScreenWiderThanExpected(resources: Resources): Boolean = resources.configuration.screenWidthDp > 560