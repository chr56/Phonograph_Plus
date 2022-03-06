/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.adapter

import android.annotation.SuppressLint
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
import util.phonograph.misc.SwipeAndDragHelper
import util.phonograph.misc.SwipeAndDragHelper.ActionCompletionContract
import java.lang.IllegalStateException
import java.lang.StringBuilder

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
                TabItem(position, config.get(position), true)
            )
            restAvailableTabs.remove(config.get(position))
                .also { if (!it) throw IllegalStateException("InvalidTab: ${config.get(position)} in ${tabs.print()}") }
        } else {
            holder.checkBox.isChecked = false
            tabs.tabItems.add(
                position,
                TabItem(position, restAvailableTabs.first(), false)
            )
            restAvailableTabs.removeFirst()
        }
        holder.title.text = PAGERS.getDisplayName(tabs.get(position)?.name, App.instance)

        setupBehavior(holder, position)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBehavior(holder: ViewHolder, position: Int) {
        holder.itemView.setOnClickListener { view ->
            val checkBox = view.findViewById<CheckBox>(R.id.checkbox)
            when {
                checkBox.isChecked -> {
                    if (isLastCheckedOne()) {
                        Toast.makeText(holder.itemView.context, R.string.you_have_to_select_at_least_one_category, Toast.LENGTH_SHORT).show()
                    } else {
                        checkBox.isChecked = false
                        tabs.toggle(tabs.get(position) ?: throw IllegalStateException("position$position is invalid!"))
                    }
                }
                !checkBox.isChecked -> {
                    checkBox.isChecked = true
                    tabs.toggle(tabs.get(position) ?: throw IllegalStateException("position$position is invalid!"))
                }
            }
        }
        holder.dragView.setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(holder)
            }
            false
        }
    }

    private fun isLastCheckedOne(): Boolean {
        return tabs.checkedItemNumbers() <= 1
    }

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

    private data class TabItem(var order: Int, var name: String, var visibility: Boolean)
    private class TabList(val tabItems: MutableList<TabItem>) {
        fun get(position: Int): TabItem? {
            for (tab in tabItems) {
                if (tab.order == position) return tab
            }
            return null
        }
        fun toggle(item: TabItem) {
            val m = item.copy().apply { visibility = visibility.not() }
            tabItems.remove(item)
            tabItems.add(m.order, m)
        }
        fun move(oldPosition: Int, newPosition: Int) {
            val m = get(oldPosition)!!.copy().apply { order = newPosition }
            tabItems.remove(get(oldPosition)!!)
            tabItems.add(newPosition, m)
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
                            hashMap.put(index, it.name)
                            index++
                        }
                    }
                }
            )
        }
        fun print(): String {
            return StringBuilder().also { s ->
                tabItems.forEach {
                    s.append("{${it.order},${it.name},${it.visibility}},")
                }
            }.toString()
        }
    }
    val currentConfig: PageConfig get() = tabs.toPageConfig()
    fun getState(): String = tabs.print()

    companion object {
        private const val TAG = "HomeTabConfigAdapter"
    }
}
