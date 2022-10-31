package player.phonograph.adapter.legacy

import android.content.Context
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import coil.size.ViewSizeResolver
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Album
import player.phonograph.model.getYearString

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
                activity.primaryTextColor(color)
            )
        }
        if (holder.text != null) {
            holder.text!!.setTextColor(
                activity.secondaryTextColor(color)
            )
        }
    }

    override fun loadAlbumCover(album: Album, holder: ViewHolder) {
        if (holder.image == null) return
        loadImage(context) {
            data(album.safeGetFirstSong())
            size(ViewSizeResolver(holder.image!!))
            target(PaletteTargetBuilder(context)
                .onStart {
                    holder.image!!.setImageResource(R.drawable.default_album_art)
                    setColors(context.getColor(R.color.defaultFooterColor), holder)
                }
                .onResourceReady { result, palette ->
                    holder.image!!.setImageDrawable(result)
                    if (usePalette) setColors(palette, holder)
                }
                .build())
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
