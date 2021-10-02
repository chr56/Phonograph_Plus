package com.kabouzeid.gramophone.adapter.song

import android.graphics.Typeface
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import chr_56.MDthemer.core.ThemeColor
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.helper.MusicPlayerRemote
import com.kabouzeid.gramophone.interfaces.CabHolder
import com.kabouzeid.gramophone.model.Song
import java.util.Locale

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class ShuffleButtonSongAdapter(
    activity: AppCompatActivity?,
    dataSet: List<Song>,
    @LayoutRes itemLayoutRes: Int,
    usePalette: Boolean,
    cabHolder: CabHolder?
) : AbsOffsetSongAdapter(
    activity!!, dataSet, itemLayoutRes, usePalette, cabHolder
) {
    override fun createViewHolder(view: View): SongAdapter.ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongAdapter.ViewHolder, position: Int) {
        if (holder.itemViewType == OFFSET_ITEM) {
            val accentColor = ThemeColor.accentColor(activity)

            holder.title?.let {
                it.text =
                    activity.resources.getString(R.string.action_shuffle_all).uppercase(Locale.getDefault())
                it.setTextColor(accentColor)
                it.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
            holder.text?.let {
                it.visibility = View.GONE
            }
            holder.menu?.let {
                it.visibility = View.GONE
            }
            holder.image?.let {
                val padding =
                    activity.resources.getDimensionPixelSize(R.dimen.default_item_margin) / 2
                it.setPadding(padding, padding, padding, padding)
                it.setColorFilter(accentColor)
                it.setImageResource(R.drawable.ic_shuffle_white_24dp)
            }
            holder.separator?.let {
                it.visibility = View.VISIBLE
            }
            holder.shortSeparator?.let {
                it.visibility = View.GONE
            }
        } else {
            super.onBindViewHolder(holder, position - 1)
        }
    }

    inner class ViewHolder(itemView: View) : AbsOffsetSongAdapter.ViewHolder(itemView) {
        override fun onClick(v: View) {
            if (itemViewType == OFFSET_ITEM) {
                MusicPlayerRemote.openAndShuffleQueue(getDataSet(), true)
                return
            }
            super.onClick(v)
        }
    }
}
