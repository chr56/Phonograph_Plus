/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.R
import util.phonograph.misc.SwipeAndDragHelper
import util.phonograph.misc.SwipeAndDragHelper.ActionCompletionContract
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.Toast

abstract class SortableListAdapter<C> :
        RecyclerView.Adapter<SortableListAdapter.ViewHolder>(),
        ActionCompletionContract {

    private lateinit var touchHelper: ItemTouchHelper
    protected lateinit var dataset: SortableList<C>

    open fun init() {
        touchHelper = ItemTouchHelper(SwipeAndDragHelper(this))
        dataset = fetchDataset()
    }


    abstract fun fetchDataset(): SortableList<C>


    override fun getItemCount(): Int = dataset.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sortable, parent, false)

        val contentView = onCreateContentView(parent, viewType)
        val container: FrameLayout = view.findViewById(R.id.content_view)
        container.addView(contentView)

        return ViewHolder(view, contentView)
    }

    protected abstract fun onCreateContentView(parent: ViewGroup, viewType: Int): View

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        /* noinspection ClickableViewAccessibility */
        holder.dragView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(holder)
            }
            false
        }

        holder.checkBox.isChecked = dataset[position].visible

        holder.itemView.setOnClickListener { view ->
            val checkBox = view.findViewById<CheckBox>(R.id.checkbox)
            when (checkBox.isChecked) {
                true  -> {
                    if (isLastCheckedOne()) {
                        Toast.makeText(
                            holder.itemView.context,
                            R.string.you_have_to_select_at_least_one_category,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        checkBox.isChecked = false
                        dataset.toggle(holder.bindingAdapterPosition)
                    }
                }
                false -> {
                    checkBox.isChecked = true
                    dataset.toggle(holder.bindingAdapterPosition)
                }
            }
        }


        onBindContentView(holder.contentView, position)
    }

    protected abstract fun onBindContentView(contentView: View, position: Int)

    class ViewHolder(rootView: View, val contentView: View) : RecyclerView.ViewHolder(rootView) {
        val checkBox: CheckBox = rootView.findViewById(R.id.checkbox)
        val dragView: View = rootView.findViewById(R.id.drag_view)
    }

    class SortableList<C>(init: List<Item<C>>) {

        private val items: MutableList<Item<C>> = init.toMutableList()
        val allItems get() = items.toList()

        operator fun get(position: Int) = items[position]

        val size get() = items.size

        fun visibleItems() = items.filter { it.visible }

        fun toggle(position: Int) {
            get(position).apply { visible = !visible }
        }

        fun move(oldPosition: Int, newPosition: Int) {
            if (oldPosition == newPosition) return // do nothing
            val item = get(oldPosition)
            items.remove(item)
            items.add(newPosition, item)
        }

        fun dump(): String = items.fold("TabItems:") { acc, item ->
            "$acc,[${item.content}(${if (item.visible) "visible" else "invisible"} )]"
        }

        data class Item<C>(
            val content: C,
            var visible: Boolean
        )
    }

    private fun isLastCheckedOne(): Boolean = dataset.visibleItems().size <= 1

    fun attachToRecyclerView(recyclerView: RecyclerView?) =
        touchHelper.attachToRecyclerView(recyclerView)

    override fun onViewMoved(oldPosition: Int, newPosition: Int) {
        dataset.move(oldPosition, newPosition)
        notifyItemMoved(oldPosition, newPosition)
    }

    fun getState(): String = dataset.dump()
}