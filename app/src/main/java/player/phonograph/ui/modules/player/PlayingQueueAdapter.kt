/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.player

import player.phonograph.model.Song
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.actions.ActionMenuProviders
import player.phonograph.ui.actions.ClickActionProviders
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.DraggableDisplayAdapter
import player.phonograph.ui.adapter.MultiSelectionController
import player.phonograph.util.produceSafeId
import player.phonograph.util.text.infoString
import player.phonograph.util.ui.hitTest
import androidx.fragment.app.FragmentActivity
import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView

class PlayingQueueAdapter(activity: FragmentActivity) :
        DraggableDisplayAdapter<Song>(activity, PlayingQueuePresenter, allowMultiSelection = false) {

    var current: Int = -1
        @SuppressLint("NotifyDataSetChanged") // number 0 is moving, meaning all items' number is changing
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemId(position: Int): Long = produceSafeId(presenter.getItemID(dataset[position]), position)

    override fun getItemViewType(position: Int): Int = when {
        position < current -> HISTORY
        position > current -> UP_NEXT
        else               -> CURRENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraggableViewHolder<Song> {
        val view = LayoutInflater.from(activity).inflate(ItemLayoutStyle.LIST.layout(), parent, false)
        return PlayingQueueViewHolder(view)
    }

    inner class PlayingQueueViewHolder(itemView: View) : DraggableViewHolder<Song>(itemView) {

        override fun bind(
            item: Song,
            position: Int,
            dataset: List<Song>,
            presenter: DisplayPresenter<Song>,
            controller: MultiSelectionController<Song>,
        ) {
            title?.ellipsize = TextUtils.TruncateAt.MIDDLE
            text?.ellipsize = TextUtils.TruncateAt.MIDDLE
            super.bind(item, position, dataset, presenter, controller)

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

        override fun loadImage(
            item: Song,
            imageType: Int,
            imageView: ImageView?,
            presenter: DisplayPresenter<Song>,
        ) {
            image?.visibility = GONE
            imageText?.visibility = VISIBLE
            imageText?.text = String.format(null, "%d", bindingAdapterPosition - current)
            imageText?.isSingleLine = false
            imageText?.maxLines = 2
        }

        override fun isSelected(item: Song, controller: MultiSelectionController<Song>): Boolean = false
    }

    object PlayingQueuePresenter : DisplayPresenter<Song> {

        override fun getItemID(item: Song): Long = item.id

        override fun getDisplayTitle(context: Context, item: Song): CharSequence = item.title

        override fun getDescription(context: Context, item: Song): CharSequence = item.infoString()
        override fun getSecondaryText(context: Context, item: Song): CharSequence = item.albumName ?: "N/A"
        override fun getTertiaryText(context: Context, item: Song): CharSequence? = item.artistName

        override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST
        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_NONE // custom
        override val usePalette: Boolean = false

        override val clickActionProvider: ClickActionProviders.ClickActionProvider<Song> =
            object : ClickActionProviders.ClickActionProvider<Song> {
                override fun listClick(
                    list: List<Song>, position: Int, context: Context, imageView: ImageView?,
                ): Boolean {
                    MusicPlayerRemote.playSongAt(position)
                    return true
                }
            }

        override val menuProvider: ActionMenuProviders.ActionMenuProvider<Song>
            get() = object : ActionMenuProviders.ActionMenuProvider<Song> {
                override fun inflateMenu(menu: Menu, context: Context, item: Song, position: Int) {}
                override fun prepareMenu(
                    menuButtonView: View,
                    item: Song,
                    bindingPosition: Int,
                ) {
                    ActionMenuProviders.SongActionMenuProvider(showPlay = false, index = bindingPosition)
                        .prepareMenu(menuButtonView, item, bindingPosition)
                }
            }
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