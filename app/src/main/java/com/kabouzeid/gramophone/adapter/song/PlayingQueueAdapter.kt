package com.kabouzeid.gramophone.adapter.song

import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.helper.MusicPlayerRemote
import com.kabouzeid.gramophone.interfaces.CabHolder
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.ViewUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class PlayingQueueAdapter(
    activity: AppCompatActivity?,
    dataSet: List<Song>,
    private var current: Int,
    @LayoutRes itemLayoutRes: Int,
    usePalette: Boolean,
    cabHolder: CabHolder?
) : SongAdapter(
    activity!!, dataSet, itemLayoutRes, usePalette, cabHolder
),
    DraggableItemAdapter<PlayingQueueAdapter.ViewHolder> {

    override fun createViewHolder(view: View): SongAdapter.ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongAdapter.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        if (holder.imageText != null) {
            holder.imageText!!.text = (position - current).toString()
        }
        if (holder.itemViewType == HISTORY || holder.itemViewType == CURRENT) {
            setAlpha(holder, 0.5f)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position < current) {
            return HISTORY
        } else if (position > current) {
            return UP_NEXT
        }
        return CURRENT
    }

    override fun loadAlbumCover(song: Song?, holder: SongAdapter.ViewHolder) {
        // We don't want to load it in this adapter
    }

    fun swapDataSet(dataSet: List<Song?>?, position: Int) {
        this.dataSet = dataSet as List<Song>
        current = position
        notifyDataSetChanged()
    }

    fun setCurrent(current: Int) {
        this.current = current
        notifyDataSetChanged()
    }

    private fun setAlpha(holder: SongAdapter.ViewHolder, alpha: Float) {
        if (holder.image != null) {
            holder.image!!.alpha = alpha
        }
        if (holder.title != null) {
            holder.title!!.alpha = alpha
        }
        if (holder.text != null) {
            holder.text!!.alpha = alpha
        }
        if (holder.imageText != null) {
            holder.imageText!!.alpha = alpha
        }
        if (holder.paletteColorContainer != null) {
            holder.paletteColorContainer!!.alpha = alpha
        }
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean {
        return ViewUtil.hitTest(holder.imageText, x, y)
    }

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange? {
        return null
    }

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        MusicPlayerRemote.moveSong(fromPosition, toPosition)
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean {
        return true
    }

    override fun onItemDragStarted(position: Int) {
        notifyDataSetChanged()
    }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) :
        SongAdapter.ViewHolder(itemView),
        DraggableItemViewHolder {

        init {
            imageText?.let {
                it.visibility = View.VISIBLE
            }
            image?.let {
                it.visibility = View.GONE
            }
        }

        @DraggableItemStateFlags
        private var mDragStateFlags = 0
        override val songMenuRes: Int
            get() = R.menu.menu_item_playing_queue_song

        override fun onSongMenuItemClick(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_remove_from_playing_queue -> {
                    MusicPlayerRemote.removeFromQueue(bindingAdapterPosition)
                    return true
                }
            }
            return super.onSongMenuItemClick(item)
        }

        override fun setDragStateFlags(@DraggableItemStateFlags flags: Int) {
            mDragStateFlags = flags
        }

        @DraggableItemStateFlags
        override fun getDragStateFlags(): Int {
            return mDragStateFlags
        }
    }

    companion object {
        private const val HISTORY = 0
        private const val CURRENT = 1
        private const val UP_NEXT = 2
    }
}
