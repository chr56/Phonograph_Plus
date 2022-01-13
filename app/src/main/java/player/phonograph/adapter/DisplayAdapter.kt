/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.R
import player.phonograph.adapter.base.MediaEntryViewClickListener
import player.phonograph.adapter.base.MultiSelectAdapter
import player.phonograph.adapter.base.UniversalMediaEntryViewHolder
import player.phonograph.interfaces.Displayable
import player.phonograph.interfaces.MultiSelectionCabProvider

class DisplayAdapter<I : Displayable>(
    private val activity: AppCompatActivity,
    host: MultiSelectionCabProvider?,
    dataSet: List<I>,
    @LayoutRes var layoutRes: Int,
    cfg: (DisplayAdapter<I>.() -> Unit)?
) :
    MultiSelectAdapter<DisplayAdapter<I>.DisplayViewHolder, I>(activity, host), FastScrollRecyclerView.SectionedAdapter {

    var dataset: List<I> = dataSet
        private set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
        cfg?.invoke(this)
    }

    var usePalette: Boolean = false

    var showSectionName: Boolean = false

    override var multiSelectMenuRes: Int = R.menu.menu_media_selection

    override fun getItemId(position: Int): Long =
        dataset[position].getItemID()

    override fun getItem(datasetPosition: Int): I {
        return dataset[datasetPosition]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder {
        return DisplayViewHolder(
            LayoutInflater.from(activity).inflate(layoutRes, parent, false)
        )
    }

    override fun onBindViewHolder(holder: DisplayViewHolder, position: Int) {
        val item: I = dataset[position]
        holder.itemView.isActivated = isChecked(item)
        holder.title?.text = item.getTitle()
        holder.text?.text = item.getDescription()
        holder.shortSeparator?.visibility = View.VISIBLE
        holder.image?.also {
            TODO()
        }
    }

    override fun getItemCount(): Int = dataset.size

    override fun updateItemCheckStatusForAll() = notifyDataSetChanged()
    override fun updateItemCheckStatus(datasetPosition: Int) = notifyItemChanged(datasetPosition)

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<I>) {
        if (dataset.isNotEmpty()) dataset[0].multiMenuHandler()?.invoke(activity, selection, menuItem.itemId)
    }

    override fun getSectionName(position: Int): String {
        return dataset[position].getSortOrderReference() ?: "-" // TODO
    }

    inner class DisplayViewHolder(itemView: View) : UniversalMediaEntryViewHolder(itemView), MediaEntryViewClickListener {

        private val displayItem = dataset[bindingAdapterPosition]

        init {
            // Item Click
            setClickListener(object : MediaEntryViewClickListener {
                override fun onLongClick(v: View): Boolean {
                    return toggleChecked(bindingAdapterPosition)
                }
                override fun onClick(v: View) {
                    when (isInQuickSelectMode) {
                        true -> toggleChecked(bindingAdapterPosition)
                        false -> dataset[0].clickHandler().invoke(displayItem, dataset)
                    }
                }
            })
            // Munu Click
            menu?.setOnClickListener { view ->
                if (dataset.isNotEmpty()) {
                    val menuRes = dataset[0].menuRes()
                    val popupMenu = PopupMenu(activity, view)
                    popupMenu.inflate(menuRes)
                    popupMenu.setOnMenuItemClickListener { menuItem ->
                        if (menuItem != null)
                            return@setOnMenuItemClickListener dataset[0].menuHandler()
                                ?.invoke(activity, displayItem, menuItem.itemId) ?: false
                        else return@setOnMenuItemClickListener false
                    }
                    popupMenu.show()
                }
            }
        }
    }
}
