/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.ui

import androidx.annotation.ColorInt
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.view.inputmethod.InputMethodManager

fun convertDpToPixel(dp: Float, resources: Resources): Float {
    val metrics = resources.displayMetrics
    return dp * metrics.density
}

fun convertPixelsToDp(px: Float, resources: Resources): Float {
    val metrics = resources.displayMetrics
    return px / metrics.density
}

fun hideKeyboard(activity: Activity?) {
    if (activity != null) {
        val currentFocus = activity.currentFocus
        if (currentFocus != null) {
            val inputMethodManager =
                activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }
}

fun createSelectorDrawable(@Suppress("UNUSED_PARAMETER") context: Context, @ColorInt color: Int): Drawable {
    val baseSelector = StateListDrawable()
    baseSelector.addState(intArrayOf(android.R.attr.state_activated), ColorDrawable(color))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        return RippleDrawable(ColorStateList.valueOf(color), baseSelector, ColorDrawable(Color.WHITE))
    }
    baseSelector.addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
    baseSelector.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(color))
    return baseSelector
}