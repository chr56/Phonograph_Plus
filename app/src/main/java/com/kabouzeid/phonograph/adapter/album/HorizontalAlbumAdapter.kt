package com.kabouzeid.phonograph.adapter.album

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import chr_56.MDthemer.util.ColorUtil
import chr_56.MDthemer.util.MaterialColorHelper
import com.bumptech.glide.Glide
import com.kabouzeid.phonograph.glide.PhonographColoredTarget
import com.kabouzeid.phonograph.glide.SongGlideRequest
import com.kabouzeid.phonograph.helper.HorizontalAdapterHelper
import com.kabouzeid.phonograph.interfaces.CabHolder
import com.kabouzeid.phonograph.model.Album
import com.kabouzeid.phonograph.util.MusicUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class HorizontalAlbumAdapter(
    activity: AppCompatActivity,
    dataSet: List<Album>,
    usePalette: Boolean,
    cabHolder: CabHolder?
) : AlbumAdapter(activity, dataSet, HorizontalAdapterHelper.LAYOUT_RES, usePalette, cabHolder) {
    override fun createViewHolder(view: View, viewType: Int): ViewHolder {
        val params = view.layoutParams as MarginLayoutParams
        HorizontalAdapterHelper.applyMarginToLayoutParams(activity, params, viewType)
        return ViewHolder(view)
    }

    override fun setColors(color: Int, holder: ViewHolder) {
        if (holder.itemView != null) {
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

    override fun getAlbumText(album: Album): String {
        return MusicUtil.getYearString(album.year)
    }

    override fun getItemViewType(position: Int): Int {
        return HorizontalAdapterHelper.getItemViewtype(position, itemCount)
    }
}