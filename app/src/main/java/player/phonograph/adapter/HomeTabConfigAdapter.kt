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
import util.phonograph.misc.SwipeAndDragHelper
import util.phonograph.misc.SwipeAndDragHelper.ActionCompletionContract

class HomeTabConfigAdapter(private val config: PageConfig) : RecyclerView.Adapter<HomeTabConfigAdapter.ViewHolder>(), ActionCompletionContract {
    private val touchHelper = ItemTouchHelper(SwipeAndDragHelper(this))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.preference_dialog_home_tab_config_listitem, parent, false))
    }

    private val tabs: TabList = TabList(ArrayList(3))
    private val restAvailableTabs: MutableList<String> =
        PageConfig.DEFAULT_CONFIG.tabMap.let {
            val list = ArrayList<String>(3)
            it.forEach { entry: Map.Entry<Int, String> ->
                list.add(entry.value)
            }
            list
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < config.getSize()) {
            holder.checkBox.isChecked = true
            tabs.tabItems.add(
                position,
                TabItem(config.get(position), true)
            )
            restAvailableTabs.remove(config.get(position))
                .also { if (!it) throw IllegalStateException("InvalidTab: ${config.get(position)} in ${tabs.print()}") }
        } else {
            holder.checkBox.isChecked = false
            tabs.tabItems.add(
                position,
                TabItem(restAvailableTabs.first(), false)
            )
            restAvailableTabs.removeFirst()
        }
        holder.title.text = Pages.getDisplayName(tabs.get(position).name, App.instance)

        holder.itemView.setOnClickListener { view ->
            val checkBox = view.findViewById<CheckBox>(R.id.checkbox)
            when (checkBox.isChecked) {
                true -> {
                    if (isLastCheckedOne()) {
                        Toast.makeText(holder.itemView.context, R.string.you_have_to_select_at_least_one_category, Toast.LENGTH_SHORT).show()
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

    private fun isLastCheckedOne(): Boolean = tabs.checkedItemNumbers() <= 1

    fun attachToRecyclerView(recyclerView: RecyclerView?) = touchHelper.attachToRecyclerView(recyclerView)

    override fun onViewMoved(oldPosition: Int, newPosition: Int) {
        tabs.move(oldPosition, newPosition)
        notifyItemMoved(oldPosition, newPosition)
    }

    override fun getItemCount(): Int = PageConfig.DEFAULT_CONFIG.getSize()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var checkBox: CheckBox = view.findViewById(R.id.checkbox)
        var title: TextView = view.findViewById(R.id.title)
        var dragView: View = view.findViewById(R.id.drag_view)
    }

    private data class TabItem(var name: String, var visibility: Boolean)
    private class TabList(val tabItems: MutableList<TabItem>) {
        fun get(position: Int): TabItem {
            return tabItems[position]
        }
        fun toggle(position: Int) {
            get(position).apply { visibility = visibility.not() }
        }
        fun move(oldPosition: Int, newPosition: Int) {
            if (oldPosition == newPosition) return // do nothing
            val item = get(oldPosition)
            tabItems.remove(item)
            tabItems.add(newPosition, item)
        }
        fun checkedItemNumbers(): Int {
            var count = 0
            tabItems.forEach {
                if (it.visibility) count++
            }
            return count
        }
        fun toPageConfig(): PageConfig {
            return PageConfig(
                HashMap<Int, String>().also { hashMap ->
                    var index = 0
                    tabItems.forEach {
                        if (it.visibility) {
                            hashMap[index] = it.name
                            index++
                        }
                    }
                }
            )
        }
        fun print(): String {
            return tabItems.map { " [name=${it.name},visibility=${it.visibility}]" }
                .fold("TabItems:") {
                        acc, s ->
                    "$acc$s"
                }
        }
    }
    val currentConfig: PageConfig get() = tabs.toPageConfig()
    fun getState(): String = tabs.print()

    companion object {
        @Suppress("unused")
        private const val TAG = "HomeTabConfigAdapter"
    }
}
