package player.phonograph.adapter.legacy

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import mt.util.color.getPrimaryTextColor
import mt.util.color.getSecondaryTextColor
import mt.util.color.isColorLight
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
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
                getPrimaryTextColor(
                    activity,
                    isColorLight(color)
                )
            )
        }
        if (holder.text != null) {
            holder.text!!.setTextColor(
                getSecondaryTextColor(
                    activity,
                   isColorLight(color)
                )
            )
        }
    }

    override fun loadAlbumCover(album: Album, holder: ViewHolder) {
        if (holder.image == null) return
        SongGlideRequest.Builder.from(Glide.with(activity), album.safeGetFirstSong())
            .checkIgnoreMediaStore(activity)
            .generatePalette(activity).build()
            .into(object : PhonographColoredTarget(holder.image) {
                override fun onLoadCleared(placeholder: Drawable?) {
                    super.onLoadCleared(placeholder)
                    setColors(albumArtistFooterColor, holder)
                }

                override fun onColorReady(color: Int) {
                    if (usePalette) setColors(color, holder) else setColors(
                        albumArtistFooterColor,
                        holder
                    )
                }
            })
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
