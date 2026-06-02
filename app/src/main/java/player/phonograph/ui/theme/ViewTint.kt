/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.theme

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import util.theme.color.withAlpha
import androidx.appcompat.R
import android.content.Context

fun FastScrollRecyclerView.setUpFastScrollRecyclerViewColor(context: Context, color: Int) {
    setPopupBgColor(color)
    setPopupTextColor(textColorOn(context, color))
    setThumbColor(color)
    setTrackColor(
        withAlpha(
            context.resolveThemeColor(
                R.attr.colorControlNormal,
                player.phonograph.R.color.default_text_color_secondary,
            ), 0.12f
        )
    )
}