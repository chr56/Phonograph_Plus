/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.util

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Activity
import android.view.View


fun hitTest(v: View, x: Int, y: Int): Boolean {
    val tx = (v.translationX + 0.5f).toInt()
    val ty = (v.translationY + 0.5f).toInt()
    val left = v.left + tx
    val right = v.right + tx
    val top = v.top + ty
    val bottom = v.bottom + ty
    return x in left..right && y >= top && y <= bottom
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