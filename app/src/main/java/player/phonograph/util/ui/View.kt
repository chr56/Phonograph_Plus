/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.ui

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import util.theme.color.primaryTextColor
import util.theme.color.withAlpha
import player.phonograph.util.theme.resolveColor
import android.content.Context
import android.util.TypedValue
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