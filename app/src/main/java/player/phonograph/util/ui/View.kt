/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.ui

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.util.theme.resolveColor
import util.theme.color.primaryTextColor
import util.theme.color.withAlpha
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup

fun hitTest(v: View, x: Int, y: Int): Boolean {
    val tx = (v.translationX + 0.5f).toInt()
    val ty = (v.translationY + 0.5f).toInt()
    val left = v.left + tx
    val right = v.right + tx
    val top = v.top + ty
    val bottom = v.bottom + ty
    return x in left..right && y >= top && y <= bottom
}

fun FastScrollRecyclerView.setUpFastScrollRecyclerViewColor(context: Context, accentColor: Int) {
    setPopupBgColor(accentColor)
    setPopupTextColor(context.primaryTextColor(accentColor))
    setThumbColor(accentColor)
    setTrackColor(
        withAlpha(
            context.resolveColor(
                androidx.appcompat.R.attr.colorControlNormal,
                context.primaryTextColor()
            ), 0.12f
        )
    )
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

/**
 * apply Edge-to-edge window insets of SystemBars for this ViewGroup that are at bottom
 * @param what insets type that used
 */
fun ViewGroup.applyWindowInsetsAsBottomView(
    @InsetsType what: Int = WindowInsetsCompat.Type.systemBars(),
) {
    if (clipToPadding) clipToPadding = false
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(what)
        view.setPadding(
            view.paddingLeft,
            view.paddingTop,
            view.paddingRight,
            insets.bottom
        )
        windowInsets
    }
}
