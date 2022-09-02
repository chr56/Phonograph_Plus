package player.phonograph.adapter.legacy

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PhonographColoredTarget
import player.phonograph.model.Album
import player.phonograph.model.getYearString
import util.mdcolor.ColorUtil
import util.mddesign.util.MaterialColorHelper

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class HorizontalAlbumAdapter(
    activity: AppCompatActivity,
    dataSet: List<Album>,
    usePalette: Boolean,
    cabController: MultiSelectionCabController,
) : AlbumAdapter(activity, dataSet, LAYOUT_RES, usePalette, cabController) {

    override fun createViewHolder(view: View, viewType: Int): ViewHolder {
        (view.layoutParams as MarginLayoutParams).applyMarginToLayoutParams(activity, viewType)
        return ViewHolder(view)
    }

    override fun setColors(color: Int, holder: ViewHolder) {
        val card = holder.itemView as CardView
        card.setCardBackgroundColor(color)
        if (holder.title != null) {
            holder.title!!.setTextColor(
                MaterialColorHelper.getPrimaryTextColor(
                    activity,
                    ColorUtil.isColorLight(color)
                )
            )
        }
        if (holder.text != null) {
            holder.text!!.setTextColor(
                MaterialColorHelper.getSecondaryTextColor(
                    activity,
                    ColorUtil.isColorLight(color)
                )
            )
        }
    }

    override fun loadAlbumCover(album: Album, holder: ViewHolder) {
        if (holder.image == null) return
        loadImage(context) {
            data(album.safeGetFirstSong())
            target(object : PhonographColoredTarget() {

                override fun onStart(placeholder: Drawable?) {
                    super.onStart(placeholder)
                    holder.image!!.setImageResource(R.drawable.default_album_art)
                    setColors(defaultFooterColor, holder)
                }

                override fun onResourcesReady(drawable: Drawable) {
                    holder.image!!.setImageDrawable(drawable)
                }

                override fun onColorReady(color: Int) {
                    if (usePalette) setColors(color, holder)
                }
            })
        }
    }

    override fun getAlbumText(album: Album): String =
        getYearString(album.year)

    override fun getItemViewType(position: Int): Int = when (position) {
        0 -> {
            TYPE_FIRST
        }
        itemCount - 1 -> {
            TYPE_LAST
        }
        else -> TYPE_MIDDLE
    }

    fun MarginLayoutParams.applyMarginToLayoutParams(context: Context, viewType: Int) {
        val listMargin = context.resources.getDimensionPixelSize(R.dimen.default_item_margin)

        if (viewType == TYPE_FIRST) {
            leftMargin = listMargin
        } else if (viewType == TYPE_LAST) {
            rightMargin = listMargin
        }
    }

    override fun getItemId(position: Int): Long = dataSet[position].id

    companion object {
        const val LAYOUT_RES = R.layout.item_grid_card_horizontal
        const val TYPE_FIRST = 1
        const val TYPE_MIDDLE = 2
        const val TYPE_LAST = 3
    }
}
