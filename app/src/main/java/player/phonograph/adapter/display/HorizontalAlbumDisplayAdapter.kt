/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.adapter.display

import player.phonograph.R
import player.phonograph.model.Album
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.content.res.Resources
import android.view.ViewGroup.MarginLayoutParams

class HorizontalAlbumDisplayAdapter(
    activity: AppCompatActivity,
    dataSet: List<Album>,
    cfg: (DisplayAdapter<Album>.() -> Unit)?,
) : AlbumDisplayAdapter(activity, dataSet, R.layout.item_grid_card_horizontal, cfg) {

    override fun setPaletteColors(color: Int, holder: DisplayViewHolder) {
        super.setPaletteColors(color, holder)
        (holder.itemView as CardView).setCardBackgroundColor(color)
    }

    override fun onBindViewHolder(holder: DisplayViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        with(holder.itemView) {
            (layoutParams as MarginLayoutParams).applyMarginToLayoutParams(resources, position)
        }
    }

    private fun MarginLayoutParams.applyMarginToLayoutParams(resources: Resources, position: Int) {
        val listMargin = resources.getDimensionPixelSize(R.dimen.default_item_margin)
        marginStart = 8
        marginEnd = 8
        when (position) {
            0             -> marginStart += listMargin
            itemCount - 1 -> marginEnd += listMargin
        }
    }

    override fun getItemViewType(position: Int): Int = when (position) {
        0             -> TYPE_FIRST
        itemCount - 1 -> TYPE_LAST
        else          -> TYPE_MIDDLE
    }

    companion object {
        private const val TYPE_FIRST = 1
        private const val TYPE_MIDDLE = 2
        private const val TYPE_LAST = 3
    }
}