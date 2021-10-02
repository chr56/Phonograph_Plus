package com.kabouzeid.gramophone.adapter.song

import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import chr_56.MDthemer.core.ThemeColor
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.interfaces.CabHolder
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.MusicUtil
import com.kabouzeid.gramophone.util.NavigationUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class PlaylistSongAdapter(
    activity: AppCompatActivity?,
    dataSet: List<Song>,
    @LayoutRes itemLayoutRes: Int,
    usePalette: Boolean,
    cabHolder: CabHolder?
) : AbsOffsetSongAdapter(
    activity!!, dataSet, itemLayoutRes, usePalette, cabHolder, false
) {
    init {
        setMultiSelectMenuRes(R.menu.menu_cannot_delete_single_songs_playlist_songs_selection)
    }

    override fun createViewHolder(view: View): SongAdapter.ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongAdapter.ViewHolder, position: Int) {
        if (holder.itemViewType == OFFSET_ITEM) {
            val textColor = ThemeColor.textColorSecondary(activity)
            holder.title?.let {
                it.text = MusicUtil.getPlaylistInfoString(activity, dataSet)
                it.setTextColor(textColor)
            }
            holder.text?.let {
                it.visibility = View.GONE
            }
            holder.menu?.let {
                it.visibility = View.GONE
            }
            holder.image?.let {
                val padding = activity.resources.getDimensionPixelSize(R.dimen.default_item_margin) / 2
                it.setPadding(padding, padding, padding, padding)
                it.setColorFilter(textColor)
                it.setImageResource(R.drawable.ic_timer_white_24dp)
            }
            holder.dragView?.let {
                it.visibility = View.GONE
            }
            holder.separator?.let{
                it.visibility = View.VISIBLE
            }
            holder.shortSeparator?.let{
                it.visibility = View.GONE
            }
        } else {
            super.onBindViewHolder(holder, position - 1)
        }
    }

    open inner class ViewHolder(itemView: View) : AbsOffsetSongAdapter.ViewHolder(itemView) {
        override val songMenuRes: Int
            get() = R.menu.menu_item_cannot_delete_single_songs_playlist_song

        override fun onSongMenuItemClick(item: MenuItem): Boolean {
            if (item.itemId == R.id.action_go_to_album) {
                val albumPairs = arrayOf<Pair<*, *>>(
                    Pair.create(image, activity.getString(R.string.transition_album_art))
                )
                NavigationUtil.goToAlbum(
                    activity,
                    dataSet[bindingAdapterPosition - 1].albumId,
                    *albumPairs
                )
                return true
            }
            return super.onSongMenuItemClick(item)
        }
    }
}
