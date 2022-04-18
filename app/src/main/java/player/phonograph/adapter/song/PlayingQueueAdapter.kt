package player.phonograph.adapter.song

import android.annotation.SuppressLint
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemState
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags
import player.phonograph.R
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.interfaces.CabHolder
import player.phonograph.model.Song
import player.phonograph.util.ViewUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class PlayingQueueAdapter(
    activity: AppCompatActivity,
    dataSet: List<Song>,
    current: Int,
    @LayoutRes itemLayoutRes: Int,
    usePalette: Boolean,
    cabHolder: CabHolder?
) : SongAdapter(
    activity, dataSet, itemLayoutRes, usePalette, cabHolder
),
    DraggableItemAdapter<PlayingQueueAdapter.ViewHolder> {

    override fun createViewHolder(view: View): SongAdapter.ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongAdapter.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.imageText?.text = (position - current).toString()
        if (holder.itemViewType == HISTORY || holder.itemViewType == CURRENT) {
            setAlpha(holder, 0.5f)
        }
    }

    var current: Int = current
        @SuppressLint("NotifyDataSetChanged") // number 0 is moving, meaning all items' number is changing
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemViewType(position: Int): Int =
        when {
            position < current -> HISTORY
            position > current -> UP_NEXT
            else -> CURRENT
        }

    override fun loadAlbumCover(song: Song, holder: SongAdapter.ViewHolder) {
        // We don't want to load it in this adapter
    }

    private fun setAlpha(holder: SongAdapter.ViewHolder, alpha: Float) {
        holder.image?.alpha = alpha
        holder.title?.alpha = alpha
        holder.text?.alpha = alpha
        holder.imageText?.alpha = alpha
        holder.paletteColorContainer?.alpha = alpha
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean =
        ViewUtil.hitTest(holder.imageText, x, y)

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

    inner class ViewHolder(itemView: View) :
        SongAdapter.ViewHolder(itemView),
        DraggableItemViewHolder {

        init {
            imageText?.visibility = View.VISIBLE
            image?.visibility = View.GONE
        }

        override val menuRes: Int get() = R.menu.menu_item_playing_queue_song

        override fun onSongMenuItemClick(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_remove_from_playing_queue -> {
                    MusicPlayerRemote.removeFromQueue(bindingAdapterPosition)
                    return true
                }
            }
            return super.onSongMenuItemClick(item)
        }

        @DraggableItemStateFlags private var mDragStateFlags = 0
        @DraggableItemStateFlags override fun getDragStateFlags(): Int = mDragStateFlags
        override fun setDragStateFlags(@DraggableItemStateFlags flags: Int) {
            mDragStateFlags = flags
        }

        override fun getDragState(): DraggableItemState {
            return DraggableItemState().apply {
                this.flags = mDragStateFlags
            }
        }
    }

    companion object {
        private const val HISTORY = 0
        private const val CURRENT = 1
        private const val UP_NEXT = 2
    }
}
