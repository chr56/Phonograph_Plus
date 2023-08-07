/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.player

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemState
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.infoString
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.MultiSelectionController
import player.phonograph.ui.adapter.hasMenu
import player.phonograph.ui.adapter.initMenu
import player.phonograph.util.ui.hitTest
import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu

class PlayingQueueAdapter(
    activity: AppCompatActivity,
    dataSet: List<Song>,
    current: Int,
) : DisplayAdapter<Song>(activity, dataSet, R.layout.item_list),
    DraggableItemAdapter<PlayingQueueAdapter.PlayingQueueViewHolder> {

    var current: Int = current
        @SuppressLint("NotifyDataSetChanged") // number 0 is moving, meaning all items' number is changing
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<Song> {
        return PlayingQueueViewHolder(inflatedView(R.layout.item_list, parent))
    }

    override fun getItemViewType(position: Int): Int =
        when {
            position < current -> HISTORY
            position > current -> UP_NEXT
            else               -> CURRENT
        }

    override val allowMultiSelection: Boolean get() = false

    inner class PlayingQueueViewHolder(itemView: View) : DisplayViewHolder<Song>(itemView), DraggableItemViewHolder {

        override fun setImage(position: Int, dataset: List<Song>, usePalette: Boolean) {}

        override fun onClick(position: Int, dataset: List<Song>, imageView: ImageView?): Boolean {
            MusicPlayerRemote.playSongAt(position)
            return true
        }

        override fun onMenuClick(dataset: List<Song>, bindingAdapterPosition: Int, menuButtonView: View) {
            if (dataset.isNotEmpty()) {
                PopupMenu(itemView.context, menuButtonView).apply {
                    dataset[bindingAdapterPosition]
                        .initMenu(itemView.context, this.menu, index = bindingAdapterPosition)
                }.show()
            }
        }

        override fun bind(
            item: Song,
            position: Int,
            dataset: List<Song>,
            controller: MultiSelectionController<Song>,
            useImageText: Boolean,
            usePalette: Boolean
        ) {

            val song = dataset[position]

            itemView.isActivated = false
            title?.text = song.title
            text?.text = song.infoString()
            title?.ellipsize = TextUtils.TruncateAt.MIDDLE
            text?.ellipsize = TextUtils.TruncateAt.MIDDLE
            image?.visibility = GONE
            imageText?.visibility = VISIBLE
            imageText?.text = (position - current).toString()

            shortSeparator?.visibility = if (bindingAdapterPosition == itemCount - 1) GONE else VISIBLE
            setAlpha(
                if (itemViewType == HISTORY || itemViewType == CURRENT) 0.5f else 1f
            )
            controller.registerClicking(itemView, position) {
                onClick(position, dataset, image)
            }
            menu?.visibility = if (item.hasMenu()) VISIBLE else GONE
            menu?.setOnClickListener {
                onMenuClick(dataset, position, it)
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

    override fun onCheckCanStartDrag(holder: PlayingQueueViewHolder, position: Int, x: Int, y: Int): Boolean =
        hitTest(holder.imageText as View, x, y)

    override fun onGetItemDraggableRange(holder: PlayingQueueViewHolder, position: Int): ItemDraggableRange? = null

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        MusicPlayerRemote.moveSong(fromPosition, toPosition)
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean = true

    override fun onItemDragStarted(position: Int) {}

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
                else                      -> notifyItemChanged(fromPosition)
            }
        }
    }

    // Playing Queue might have multiple items of SAME song, so we have to make differences
    override fun getItemId(position: Int): Long =
        super.getItemId(position) + position * 65537 // 65537, the fifth Fermat prime


    companion object {
        private const val HISTORY = 0
        private const val CURRENT = 1
        private const val UP_NEXT = 2
    }
}
