/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import coil.size.ViewSizeResolver
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Album
import player.phonograph.model.buildInfoString
import player.phonograph.model.getYearString
import player.phonograph.model.songCountString
import player.phonograph.ui.fragments.pages.adapter.AlbumDisplayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams

class HorizontalAlbumDisplayAdapter(
    activity: AppCompatActivity,
    dataSet: List<Album>,
) : AlbumDisplayAdapter(activity, dataSet, R.layout.item_grid_card_horizontal) {


    override fun onBindViewHolder(holder: DisplayViewHolder<Album>, position: Int) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<Album> {
        return HorizontalAlbumViewHolder(inflatedView(layoutRes, parent))
    }

    class HorizontalAlbumViewHolder(itemView: View) : DisplayViewHolder<Album>(itemView) {

        override fun getDescription(item: Album): CharSequence {
            return buildInfoString(getYearString(item.year), songCountString(itemView.context, item.songCount))
        }

        override fun setPaletteColors(color: Int) {
            super.setPaletteColors(color)
            (itemView as CardView).setCardBackgroundColor(color)
        }

        override fun setImage(position: Int, dataset: List<Album>, usePalette: Boolean) {
            val context = itemView.context
            image?.let { view ->
                loadImage(context) {
                    data(dataset[position].safeGetFirstSong())
                    size(ViewSizeResolver(view))
                    target(
                        PaletteTargetBuilder(context)
                            .onStart {
                                view.setImageResource(R.drawable.default_album_art)
                                setPaletteColors(context.getColor(R.color.defaultFooterColor))
                            }
                            .onResourceReady { result, palette ->
                                view.setImageDrawable(result)
                                if (usePalette) setPaletteColors(palette)
                            }
                            .build()
                    )
                }
            }
        }
    }

    companion object {
        private const val TYPE_FIRST = 1
        private const val TYPE_MIDDLE = 2
        private const val TYPE_LAST = 3
    }
}