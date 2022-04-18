package player.phonograph.helper

import android.content.Context
import android.view.ViewGroup.MarginLayoutParams
import player.phonograph.R

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object HorizontalAdapterHelper {

    const val LAYOUT_RES = R.layout.item_grid_card_horizontal
    const val TYPE_FIRST = 1
    const val TYPE_MIDDLE = 2
    const val TYPE_LAST = 3

    fun applyMarginToLayoutParams(context: Context, layoutParams: MarginLayoutParams, viewType: Int) {
        val listMargin = context.resources.getDimensionPixelSize(R.dimen.default_item_margin)

        if (viewType == TYPE_FIRST) {
            layoutParams.leftMargin = listMargin
        } else if (viewType == TYPE_LAST) {
            layoutParams.rightMargin = listMargin
        }
    }

    fun getItemViewType(position: Int, itemCount: Int): Int {
        return when (position) {
            0 -> {
                TYPE_FIRST
            }
            itemCount - 1 -> {
                TYPE_LAST
            }
            else -> TYPE_MIDDLE
        }
    }
}
