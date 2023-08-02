package player.phonograph.adapter.legacy

import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.model.Album
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.content.Context
import android.view.View
import android.view.ViewGroup.MarginLayoutParams

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class HorizontalAlbumAdapter(
    activity: AppCompatActivity,
    dataSet: List<Album>,
    usePalette: Boolean,
    cabController: MultiSelectionCabController,
) : AlbumAdapter(activity, dataSet, R.layout.item_grid_card_horizontal, usePalette, cabController) {

    override fun createViewHolder(view: View, viewType: Int): ViewHolder {
        (view.layoutParams as MarginLayoutParams).applyMarginToLayoutParams(activity, viewType)
        return ViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int = when (position) {
        0             -> TYPE_FIRST
        itemCount - 1 -> TYPE_LAST
        else          -> TYPE_MIDDLE
    }

    private fun MarginLayoutParams.applyMarginToLayoutParams(context: Context, viewType: Int) {
        val listMargin = context.resources.getDimensionPixelSize(R.dimen.default_item_margin)
        if (viewType == TYPE_FIRST) {
            leftMargin = listMargin
        } else if (viewType == TYPE_LAST) {
            rightMargin = listMargin
        }
    }

    class ViewHolder(itemView: View) : AlbumAdapter.ViewHolder(itemView) {
        override fun setColors(context: Context, color: Int) {
            super.setColors(context, color)
            (itemView as CardView).setCardBackgroundColor(color)
        }
    }

    companion object {
        const val TYPE_FIRST = 1
        const val TYPE_MIDDLE = 2
        const val TYPE_LAST = 3
    }
}
