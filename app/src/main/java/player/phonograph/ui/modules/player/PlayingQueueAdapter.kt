/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.player

import player.phonograph.model.Song
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.actions.ActionMenus
import player.phonograph.ui.actions.ItemClickActions
import player.phonograph.ui.adapter.DisplayOptions
import player.phonograph.ui.adapter.DisplayOptions.Companion.ImageryType
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.DraggableDisplayAdapter
import player.phonograph.ui.adapter.MultiSelectionController
import player.phonograph.ui.adapter.SongDisplayPresenter
import player.phonograph.ui.resource.Layouts
import player.phonograph.ui.util.hitTest
import androidx.fragment.app.FragmentActivity
import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

class PlayingQueueAdapter(activity: FragmentActivity) :
        DraggableDisplayAdapter<Song>(
            activity,
            PlayingQueuePresenter,
            DisplayOptions(
                ItemLayoutStyle.LIST, null, false,
                imageryType = DisplayOptions.CUSTOM_TEXT,
            ),
            allowMultiSelection = false
        ) {

    var current: Int = -1
        set(value) {
            if (field == value) return
            field = value
            @SuppressLint("NotifyDataSetChanged")
            notifyDataSetChanged() // number 0 is moving, so all item numbers would be shifted!
        }

    override fun getItemViewType(position: Int): Int = when {
        position < current -> HISTORY
        position > current -> UP_NEXT
        else               -> CURRENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraggableViewHolder<Song> {
        val itemLayout = Layouts.itemLayoutStyle(ItemLayoutStyle.LIST)
        val view = LayoutInflater.from(activity).inflate(itemLayout, parent, false)
        return PlayingQueueViewHolder(view)
    }

    inner class PlayingQueueViewHolder(itemView: View) : DraggableViewHolder<Song>(itemView) {

        override fun bind(
            item: Song,
            position: Int,
            dataset: List<Song>,
            presenter: DisplayPresenter<Song>,
            controller: MultiSelectionController<Song>,
            options: DisplayOptions,
        ) {
            title?.ellipsize = TextUtils.TruncateAt.MIDDLE
            text?.ellipsize = TextUtils.TruncateAt.MIDDLE
            super.bind(item, position, dataset, presenter, controller, options)

            shortSeparator?.visibility = if (bindingAdapterPosition == dataset.size - 1) GONE else VISIBLE

            applyAlpha(if (itemViewType == HISTORY || itemViewType == CURRENT) 0.5f else 1f)
        }

        private fun applyAlpha(alpha: Float) {
            image?.alpha = alpha
            title?.alpha = alpha
            text?.alpha = alpha
            imageText?.alpha = alpha
            paletteColorContainer?.alpha = alpha
        }

        override fun customText(): String =
            String.format(null, "%d", bindingAdapterPosition - current)

        override fun isSelected(item: Song, controller: MultiSelectionController<Song>): Boolean = false
    }

    object PlayingQueuePresenter : SongDisplayPresenter() {

        override fun clickAction(context: Context, item: Song, position: Int, list: List<Song>): ActionEntry? =
            ItemClickActions.PlayingQueue(position)

        override fun menuEntries(context: Context, item: Song, position: Int): List<ActionEntry> =
            ActionMenus.Song(context, item, showPlay = false, index = position)

    }

    //region Draggable
    override fun editableMode(): Boolean = true

    override fun onCheckCanStartDrag(holder: DraggableViewHolder<Song>, position: Int, x: Int, y: Int): Boolean =
        hitTest(holder.imageText as View, x, y)

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        MusicPlayerRemote.queueManager.moveSong(fromPosition, toPosition)
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean = true

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
    //endregion

    companion object {
        private const val HISTORY = 0
        private const val CURRENT = 1
        private const val UP_NEXT = 2
    }
}
