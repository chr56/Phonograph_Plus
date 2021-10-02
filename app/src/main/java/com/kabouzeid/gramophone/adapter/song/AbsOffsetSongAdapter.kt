package com.kabouzeid.gramophone.adapter.song

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.helper.MusicPlayerRemote
import com.kabouzeid.gramophone.interfaces.CabHolder
import com.kabouzeid.gramophone.model.Song

/**
 * @author Eugene Cheung (arkon)
 */
abstract class AbsOffsetSongAdapter(
    activity: AppCompatActivity,
    dataSet: List<Song>,
    @LayoutRes itemLayoutRes: Int,
    usePalette: Boolean,
    cabHolder: CabHolder?
) : SongAdapter(activity, dataSet, itemLayoutRes, usePalette, cabHolder) {
    constructor(
        activity: AppCompatActivity,
        dataSet: List<Song>,
        @LayoutRes itemLayoutRes: Int,
        usePalette: Boolean,
        cabHolder: CabHolder?,
        showSectionName: Boolean
    ) : this(activity, dataSet, itemLayoutRes, usePalette, cabHolder){
        super.showSectionName = showSectionName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongAdapter.ViewHolder {
        if (viewType == OFFSET_ITEM) {
            val view =
                LayoutInflater.from(activity).inflate(R.layout.item_list_single_row, parent, false)
            return createViewHolder(view)
        }
        return super.onCreateViewHolder(parent, viewType)
    }

    override fun createViewHolder(view: View): SongAdapter.ViewHolder {
        return ViewHolder(view)
    }

    override fun getItemId(position: Int): Long {
        var position = position
        position--
        return if (position < 0) -2 else super.getItemId(position)
    }

    override fun getIdentifier(position: Int): Song {
        var position = position
        position--
        return if (position < 0) super.getIdentifier(0)
        else super.getIdentifier(position)
    }

    override fun getItemCount(): Int {
        val superItemCount = super.getItemCount()
        return if (superItemCount == 0) 0 else superItemCount + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) OFFSET_ITEM else SONG
    }

    override fun getSectionName(position: Int): String {
        var position = position
        position--
        return if (position < 0) "" else super.getSectionName(position)
    }

    open inner class ViewHolder(itemView: View) : SongAdapter.ViewHolder(itemView) {
        // could also return null, just to be safe return empty song
        override val song: Song
            get() = if (itemViewType == OFFSET_ITEM) Song.EMPTY_SONG else dataSet[adapterPosition - 1]
        // could also return null, just to be safe return empty song

        override fun onClick(v: View) {
            if (isInQuickSelectMode && itemViewType != OFFSET_ITEM) {
                toggleChecked(adapterPosition)
            } else {
                MusicPlayerRemote.openQueue(dataSet, adapterPosition - 1, true)
            }
        }

        override fun onLongClick(view: View): Boolean {
            if (itemViewType == OFFSET_ITEM) return false
            toggleChecked(adapterPosition)
            return true
        }
    }

    companion object {
        const val OFFSET_ITEM = 0
        const val SONG = 1
    }
}
