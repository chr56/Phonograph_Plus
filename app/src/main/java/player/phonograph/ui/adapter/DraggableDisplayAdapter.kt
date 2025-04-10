/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.adapter

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemState
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.util.ui.hitTest
import androidx.fragment.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

abstract class DraggableDisplayAdapter<I>(
    activity: FragmentActivity,
    presenter: DisplayPresenter<I>,
) : DisplayAdapter<I>(activity, presenter), DraggableItemAdapter<DraggableDisplayAdapter.DraggableViewHolder<I>> {

    abstract fun editableMode(): Boolean

    //region Draggable
    override fun onCheckCanStartDrag(holder: DraggableViewHolder<I>, position: Int, x: Int, y: Int): Boolean =
        editableMode() && (hitTest(holder.dragView!!, x, y) || hitTest(holder.image!!, x, y))

    override fun onGetItemDraggableRange(holder: DraggableViewHolder<I>, position: Int): ItemDraggableRange? = null

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean =
        (dropPosition >= 0) && (dropPosition <= dataset.size - 1)

    override fun onItemDragStarted(position: Int) {}

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        @Suppress("KotlinConstantConditions") when {
            fromPosition < toPosition  -> notifyItemRangeChanged(fromPosition, toPosition)
            fromPosition > toPosition  -> notifyItemRangeChanged(toPosition, fromPosition)
            fromPosition == toPosition -> notifyItemChanged(fromPosition)
        }

        // special for top and bottle
        if (fromPosition == 0) {
            notifyItemChanged(toPosition)
        }
        if (fromPosition == dataset.size - 1 && toPosition == 0) {
            notifyItemChanged(dataset.size - 1)
        }
    }
    //endregion

    override fun getItemViewType(position: Int): Int = presenter.layoutStyle.ordinal
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<I> {
        val view = LayoutInflater.from(activity).inflate(ItemLayoutStyle.from(viewType).layout(), parent, false)
        return DraggableViewHolder(view)
    }

    open class DraggableViewHolder<I>(itemView: View) : DisplayViewHolder<I>(itemView), DraggableItemViewHolder {
        //region DragState
        @DraggableItemStateFlags
        private var mDragStateFlags = 0

        @DraggableItemStateFlags
        override fun getDragStateFlags(): Int = mDragStateFlags
        override fun setDragStateFlags(@DraggableItemStateFlags flags: Int) {
            mDragStateFlags = flags
        }

        override fun getDragState(): DraggableItemState {
            return DraggableItemState().apply { this.flags = mDragStateFlags }
        }
        //endregion
    }
}