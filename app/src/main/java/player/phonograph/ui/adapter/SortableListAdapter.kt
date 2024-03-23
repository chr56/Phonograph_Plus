/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import lib.phonograph.misc.SwipeAndDragHelper
import lib.phonograph.misc.SwipeAndDragHelper.ActionCompletionContract
import player.phonograph.R
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout

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

        holder.checkBox.isChecked = dataset[position].checked

        if (clickByCheckboxOnly) {
            holder.checkBox.setOnClickListener {
                dataset.toggle(holder.bindingAdapterPosition)
            }
        } else {
            holder.itemView.setOnClickListener {
                dataset.toggle(holder.bindingAdapterPosition)
                val checkBox = it.findViewById<CheckBox>(R.id.checkbox)
                checkBox.isChecked = !checkBox.isChecked
            }
        }

        onBindContentView(holder.contentView, position)
    }

    protected open val clickByCheckboxOnly: Boolean = false

    protected abstract fun onBindContentView(contentView: View, position: Int)

    class ViewHolder(rootView: View, val contentView: View) : RecyclerView.ViewHolder(rootView) {
        val checkBox: CheckBox = rootView.findViewById(R.id.checkbox)
        val dragView: View = rootView.findViewById(R.id.drag_view)
    }

    class SortableList<C>(private val _items: MutableList<Item<C>>) : List<SortableList.Item<C>> by _items {

        constructor(from: Collection<Item<C>>) : this(from.toMutableList())

        val items get() = _items.toList()

        val checkedItems get() = _items.filter { it.checked }

        fun toggle(position: Int) {
            get(position).apply { checked = !checked }
        }

        fun move(oldPosition: Int, newPosition: Int) {
            if (oldPosition == newPosition) return // do nothing
            val item = get(oldPosition)
            _items.remove(item)
            _items.add(newPosition, item)
        }

        fun dump(): String = _items.joinToString(prefix = "TabItems:") { item ->
            "[${item.content}(${if (item.checked) "checked" else "unchecked"})]"
        }

        data class Item<C>(
            val content: C,
            var checked: Boolean,
        )
    }

    fun attachToRecyclerView(recyclerView: RecyclerView?) =
        touchHelper.attachToRecyclerView(recyclerView)

    override fun onViewMoved(oldPosition: Int, newPosition: Int) {
        dataset.move(oldPosition, newPosition)
        notifyItemMoved(oldPosition, newPosition)
    }

    fun getState(): String = dataset.dump()
}