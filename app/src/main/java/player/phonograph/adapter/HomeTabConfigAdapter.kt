/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.pages.PageConfig
import player.phonograph.model.pages.Pages
import player.phonograph.util.Util
import util.phonograph.misc.SwipeAndDragHelper
import util.phonograph.misc.SwipeAndDragHelper.ActionCompletionContract

class HomeTabConfigAdapter(config: PageConfig) :
        RecyclerView.Adapter<HomeTabConfigAdapter.ViewHolder>(),
        ActionCompletionContract {

    private val touchHelper = ItemTouchHelper(SwipeAndDragHelper(this))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.preference_dialog_home_tab_config_listitem, parent, false
                )
        )
    }

    override fun getItemCount(): Int = PageConfig.DEFAULT_CONFIG.getSize()
    private val tabs: Tabs = Tabs.from(config)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.checkBox.isChecked = tabs.get(position).visibility
        holder.title.text = Pages.getDisplayName(tabs.get(position).name, App.instance)

        holder.itemView.setOnClickListener { view ->
            val checkBox = view.findViewById<CheckBox>(R.id.checkbox)
            when (checkBox.isChecked) {
                true -> {
                    if (isLastCheckedOne()) {
                        Toast.makeText(
                            holder.itemView.context,
                            R.string.you_have_to_select_at_least_one_category,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        checkBox.isChecked = false
                        tabs.toggle(holder.bindingAdapterPosition)
                    }
                }
                false -> {
                    checkBox.isChecked = true
                    tabs.toggle(holder.bindingAdapterPosition)
                }
            }
        }
        /* noinspection ClickableViewAccessibility */
        holder.dragView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(holder)
            }
            false
        }
    }

    private fun isLastCheckedOne(): Boolean = tabs.visibleItems().size <= 1

    fun attachToRecyclerView(recyclerView: RecyclerView?) =
        touchHelper.attachToRecyclerView(recyclerView)

    override fun onViewMoved(oldPosition: Int, newPosition: Int) {
        tabs.move(oldPosition, newPosition)
        notifyItemMoved(oldPosition, newPosition)
    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var checkBox: CheckBox = view.findViewById(R.id.checkbox)
        var title: TextView = view.findViewById(R.id.title)
        var dragView: View = view.findViewById(R.id.drag_view)
    }

    private data class TabItem(var name: String, var visibility: Boolean)

    private class Tabs(val tabItems: MutableList<TabItem>) {

        fun get(position: Int): TabItem = tabItems[position]

        fun toggle(position: Int) {
            get(position).apply { visibility = visibility.not() }
        }

        fun move(oldPosition: Int, newPosition: Int) {
            if (oldPosition == newPosition) return // do nothing
            val item = get(oldPosition)
            tabItems.remove(item)
            tabItems.add(newPosition, item)
        }

        fun visibleItems() = tabItems.filter { it.visibility }

        fun toPageConfig(): PageConfig = PageConfig.from(
            visibleItems().map { it.name }
        )

        fun dump(): String = tabItems.fold("TabItems:") { acc, item ->
            "$acc,[name=${item.name},visibility=${item.visibility}]"
        }

        companion object {
            fun from(pageConfig: PageConfig): Tabs {
                val all = PageConfig.DEFAULT_CONFIG.toMutableSet()
                all.removeAll(pageConfig.tabList.toSet())
                    .report("Strange PageConfig: $pageConfig")
                val visible = pageConfig.map { TabItem(it, true) }
                val invisible = all.toList().map { TabItem(it, false) }
                return Tabs((visible + invisible).toMutableList())
            }
        }
    }

    val currentConfig: PageConfig get() = tabs.toPageConfig()
    fun getState(): String = tabs.dump()

    companion object {
        private const val TAG = "HomeTabConfigAdapter"
        private fun Boolean.report(msg: String): Boolean {
            if (!this) Util.warning(TAG, msg)
            return this
        }
    }
}
