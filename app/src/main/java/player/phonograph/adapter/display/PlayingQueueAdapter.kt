/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.adapter.display

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemState
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil
import player.phonograph.util.ViewUtil
import player.phonograph.util.menu.onSongMenuItemClick

class PlayingQueueAdapter(
    activity: AppCompatActivity,
    dataSet: List<Song>,
    current: Int,
    cfg: (DisplayAdapter<Song>.() -> Unit)?
) : DisplayAdapter<Song>(activity, null, dataSet, R.layout.item_list, cfg), DraggableItemAdapter<PlayingQueueAdapter.ViewHolder> {

    var current: Int = current
        @SuppressLint("NotifyDataSetChanged") // number 0 is moving, meaning all items' number is changing
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_list, parent, false))
    }

    override fun getItemViewType(position: Int): Int =
        when {
            position < current -> HISTORY
            position > current -> UP_NEXT
            else -> CURRENT
        }

    override fun onBindViewHolder(
        holder: DisplayViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        (holder as ViewHolder).bind(position)
    }

    override fun setImage(holder: DisplayViewHolder, position: Int) { }

    fun onClick(view: View, bindingAdapterPosition: Int) {
        if (Setting.instance.keepPlayingQueueIntact)
            MusicPlayerRemote.playNow(dataset)
        else
            MusicPlayerRemote.openQueue(dataset, bindingAdapterPosition, true)
    }

    override fun onMenuClick(bindingAdapterPosition: Int, menuButtonView: View) {
        if (dataset.isNotEmpty()) {
            val popupMenu = PopupMenu(activity, menuButtonView)
            popupMenu.inflate(R.menu.menu_item_playing_queue_song)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                if (menuItem != null) {
                    onMenuItemClick(menuItem, bindingAdapterPosition)
                } else false
            }
            popupMenu.show()
        }
    }

    private fun onMenuItemClick(menuItem: MenuItem, position: Int): Boolean {
        return when (menuItem.itemId) {
            R.id.action_go_to_album -> {
                NavigationUtil.goToAlbum(activity, dataset[position].albumId)
                return true
            }
            R.id.action_remove_from_playing_queue -> {
                MusicPlayerRemote.removeFromQueue(position)
                return true
            }
            else -> onSongMenuItemClick(activity, dataset[position], menuItem.itemId)
        }
    }

    inner class ViewHolder(itemView: View) : DisplayViewHolder(itemView), DraggableItemViewHolder {

        init {
            itemView.setOnClickListener {
                onClick(it, bindingAdapterPosition)
            }
            itemView.setOnLongClickListener { true }

            image?.visibility = GONE

            imageText?.visibility = VISIBLE

            menu?.setOnClickListener {
                onMenuClick(bindingAdapterPosition, it)
            }
        }

        fun bind(position: Int) {
            val song = dataset[position]

            itemView.isActivated = false
            title?.text = song.title
            text?.text = MusicUtil.getSongInfoString(song)
            imageText?.text = (position - current).toString()

            shortSeparator?.visibility = if (bindingAdapterPosition == itemCount - 1) GONE else VISIBLE
            if (itemViewType == HISTORY || itemViewType == CURRENT) {
                setAlpha(0.5f)
            } else {
                setAlpha(1f)
            }
        }

        private fun setAlpha(alpha: Float) {
            image?.alpha = alpha
            title?.alpha = alpha
            text?.alpha = alpha
            imageText?.alpha = alpha
            paletteColorContainer?.alpha = alpha
        }

        @DraggableItemStateFlags
        private var mDragStateFlags = 0
        @DraggableItemStateFlags
        override fun getDragStateFlags(): Int = mDragStateFlags
        override fun setDragStateFlags(@DraggableItemStateFlags flags: Int) {
            mDragStateFlags = flags
        }

        override fun getDragState(): DraggableItemState =
            DraggableItemState().apply {
                this.flags = mDragStateFlags
            }
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean =
        ViewUtil.hitTest(holder.imageText as View, x, y)

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange? = null

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        MusicPlayerRemote.moveSong(fromPosition, toPosition)
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean = true

    override fun onItemDragStarted(position: Int) { }

    @SuppressLint("NotifyDataSetChanged")
    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        if (current in fromPosition..toPosition) {
            // number 0 is moving
            notifyDataSetChanged()
        } else {
            // number 0 is not moved
            when {
                fromPosition < toPosition -> notifyItemRangeChanged(fromPosition, toPosition)
                fromPosition > toPosition -> notifyItemRangeChanged(toPosition, fromPosition)
                else -> notifyItemChanged(fromPosition)
            }
        }
    }

    companion object {
        private const val HISTORY = 0
        private const val CURRENT = 1
        private const val UP_NEXT = 2
    }
}
