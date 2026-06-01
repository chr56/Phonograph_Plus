/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.ui

import androidx.annotation.ColorInt
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
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
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.Window

fun convertDpToPixel(dp: Float, resources: Resources): Float {
    val metrics = resources.displayMetrics
    return dp * metrics.density
}

fun convertPixelsToDp(px: Float, resources: Resources): Float {
    val metrics = resources.displayMetrics
    return px / metrics.density
}

/**
 * @param view focused view
 */
fun hideKeyboard(activity: Activity, view: View? = null) {
    val currentFocus = view ?: activity.currentFocus
    if (currentFocus != null) {
        val windowController = WindowCompat.getInsetsController(activity.window, currentFocus)
        windowController.hide(WindowInsetsCompat.Type.ime())
    }
}

/**
 * @param view focused view
 */
fun showKeyboard(activity: Activity, view: View? = null) {
    val currentFocus = view ?: activity.currentFocus
    if (currentFocus != null) {
        val windowController = WindowCompat.getInsetsController(activity.window, currentFocus)
        windowController.show(WindowInsetsCompat.Type.ime())
    }
}

fun hitTest(v: View, x: Int, y: Int): Boolean {
    val tx = (v.translationX + 0.5f).toInt()
    val ty = (v.translationY + 0.5f).toInt()
    val left = v.left + tx
    val right = v.right + tx
    val top = v.top + ty
    val bottom = v.bottom + ty
    return x in left..right && y >= top && y <= bottom
}


fun getActionBarSize(context: Context): Int {
    val typedValue = TypedValue()
    val textSizeAttr = intArrayOf(androidx.appcompat.R.attr.actionBarSize)
    val indexOfAttrTextSize = 0
    val a = context.obtainStyledAttributes(typedValue.data, textSizeAttr)
    val actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1)
    a.recycle()
    return actionBarSize
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

fun DialogFragment.applyLargeDialog(ratio: Float = 0.9f) {
    applyLargeDialog(requireDialog().window!!, requireActivity().window!!, ratio)
}

fun applyLargeDialog(dialogWindows: Window, activityWindows: Window, ratio: Float) {
    dialogWindows.attributes = dialogWindows.attributes.apply {
        width = (activityWindows.decorView.width * ratio).toInt()
        height = (activityWindows.decorView.height * ratio).toInt()
    }
}

fun menuProvider(block: (Menu) -> Unit): MenuProvider {
    return object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = block.invoke(menu)

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
    }
}

fun menuProvider(block: (Menu) -> Unit, callback: (MenuItem) -> Boolean): MenuProvider {
    return object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = block.invoke(menu)

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = callback(menuItem)
    }
}