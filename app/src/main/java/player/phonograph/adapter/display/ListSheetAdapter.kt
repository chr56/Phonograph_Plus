/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.R
import player.phonograph.adapter.base.MediaEntryViewClickListener
import player.phonograph.adapter.base.MultiSelectAdapter
import player.phonograph.adapter.base.UniversalMediaEntryViewHolder
import player.phonograph.interfaces.Displayable
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.model.Song
import player.phonograph.util.MusicUtil
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.MaterialColorHelper
import util.mddesign.util.TintHelper

class ListSheetAdapter<I : Displayable>(
    private val activity: AppCompatActivity,
    host: MultiSelectionCabProvider?,
    dataSet: List<I>,
    dashboard: Dashboard,
    @LayoutRes var itemLayoutRes: Int,
    @LayoutRes var headerLayoutRes: Int,
    cfg: (ListSheetAdapter<I>.() -> Unit)?
) : MultiSelectAdapter<ListSheetAdapter.ViewHolder, I>(activity, host), FastScrollRecyclerView.SectionedAdapter {

    var dataset: List<I> = dataSet
        set(value) {
            field = value
            notifyDataSetChanged()
            dashboard.count = value.size
            if (value.isNotEmpty()) {
                @Suppress("UNCHECKED_CAST")
                dashboard.duration = when (value[0]) {
                    is Song -> { MusicUtil.getTotalDuration(activity, dataset as List<Song>) }
                    else -> 0
                } // todo
            }
        }

    var dashboard: Dashboard = dashboard
        set(value) {
            field = value
            updateDashboardText()
        }

    init {
        setHasStableIds(true)
        cfg?.invoke(this)
    }

//    var usePalette: Boolean = false

    var showSectionName: Boolean = false

    override var multiSelectMenuRes: Int = R.menu.menu_media_selection

    override fun getItemId(position: Int): Long =
        if (position > 0) dataset[position - 1].getItemID() else -1

    override fun getItem(datasetPosition: Int): I = dataset[datasetPosition]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            DASHBOARD -> DashboardHolder(LayoutInflater.from(activity).inflate(headerLayoutRes, parent, false))
            ITEM -> ItemHolder(LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false))
            else -> ViewHolder(LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false))
        }
    }
    override fun getItemViewType(position: Int): Int =
        if (position == 0) DASHBOARD else ITEM

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder.isItem) {
            val item: I = dataset[position - 1] // Dashboard
            holder.itemView.isActivated = isChecked(item)
            holder.title?.text = item.getDisplayTitle()
            holder.text?.text = item.getDescription()
            holder.shortSeparator?.visibility = View.VISIBLE
            holder.image?.let {
                loadImageImpl?.invoke(it, dataset[position - 1])
            }
        } else {
            val primaryColor = ThemeColor.primaryColor(activity)
            holder.itemView.findViewById<ConstraintLayout>(R.id.header)?.background = ColorDrawable(primaryColor)

            val textColor = MaterialColorHelper.getSecondaryTextColor(activity, ColorUtil.isColorLight(primaryColor))
            val iconColor = MaterialColorHelper.getSecondaryDisabledTextColor(activity, ColorUtil.isColorLight(primaryColor))

            TintHelper.setTint(holder.itemView.findViewById<ImageView>(R.id.name_icon), iconColor)
            TintHelper.setTint(holder.itemView.findViewById<ImageView>(R.id.song_count_icon), iconColor)
            TintHelper.setTint(holder.itemView.findViewById<ImageView>(R.id.duration_icon), iconColor)
            TintHelper.setTint(holder.itemView.findViewById<ImageView>(R.id.path_icon), iconColor)

            holder.itemView.findViewById<TextView>(R.id.name_text).setTextColor(textColor)
            holder.itemView.findViewById<TextView>(R.id.song_count_text).setTextColor(textColor)
            holder.itemView.findViewById<TextView>(R.id.duration_text).setTextColor(textColor)
            holder.itemView.findViewById<TextView>(R.id.path_text).setTextColor(textColor)
            holder.itemView.findViewById<ImageView>(R.id.icon)
                .also {
                    it.setImageDrawable(
                        TintHelper.createTintedDrawable(
                            AppCompatResources.getDrawable(activity, R.drawable.ic_queue_music_white_24dp), textColor
                        )
                    )
                }

            nameText = holder.itemView.findViewById(R.id.name_text)
            countText = holder.itemView.findViewById(R.id.song_count_text)
            durationText = holder.itemView.findViewById(R.id.duration_text)
            pathText = holder.itemView.findViewById(R.id.path_text)

            updateDashboardText()
        }
    }

    private var nameText: TextView? = null
    private var countText: TextView? = null
    private var durationText: TextView? = null
    private var pathText: TextView? = null

    fun updateDashboardText() {
        nameText?.text = dashboard.name
        countText?.text = dashboard.count.toString()
        durationText?.text = MusicUtil.getReadableDurationString(dashboard.duration)

        pathText?.text =
            if (dashboard.path.isNullOrEmpty()) {
                "-"
            } else {
                dashboard.path
            }
    }

    var loadImageImpl: ((imageView: ImageView, item: I) -> Unit)? = null

    override fun updateItemCheckStatusForAll() = notifyDataSetChanged()
    override fun updateItemCheckStatus(datasetPosition: Int) = notifyItemChanged(datasetPosition + 1) // dashboard

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<I>) {
        if (dataset.isNotEmpty()) dataset[0].multiMenuHandler()?.invoke(activity, selection, menuItem.itemId)
    }

    override fun getSectionName(position: Int): String =
        if (showSectionName && dataset.isNotEmpty()) dataset[position - 1].getDisplayTitle().toString() else "-"
    override fun getItemCount(): Int = dataset.size + 1

//    protected fun setPaletteColors(color: Int, holder: ViewHolder) {
//        holder.paletteColorContainer?.let { paletteColorContainer ->
//            paletteColorContainer.setBackgroundColor(color)
//            holder.title?.setTextColor(MaterialColorHelper.getPrimaryTextColor(activity, ColorUtil.isColorLight(color)))
//            holder.text?.setTextColor(MaterialColorHelper.getSecondaryTextColor(activity, ColorUtil.isColorLight(color)))
//        }
//    }

    companion object {
        const val DASHBOARD = 1
        const val ITEM = 0
    }

    open class ViewHolder(itemView: View) : UniversalMediaEntryViewHolder(itemView) {
        open val isItem: Boolean = true
    }
    inner class ItemHolder(itemView: View) : ViewHolder(itemView) {
        override val isItem: Boolean = true

        init {
            // Item Click
            setClickListener(object : MediaEntryViewClickListener {
                override fun onLongClick(v: View): Boolean {
                    return toggleChecked(bindingAdapterPosition - 1) // dashboard
                }
                override fun onClick(v: View) {
                    when (isInQuickSelectMode) {
                        true -> toggleChecked(bindingAdapterPosition - 1) // dashboard
                        false -> dataset[0].clickHandler().invoke(activity, dataset[bindingAdapterPosition - 1], dataset, null)
                    }
                }
            })
            // Menu Click
            if (dataset[0].menuRes() == 0 || dataset[0].menuHandler() == null) {
                menu?.visibility = View.GONE
            } else {
                menu?.setOnClickListener { view ->
                    if (dataset.isNotEmpty()) {
                        val menuRes = dataset[0].menuRes()
                        val popupMenu = PopupMenu(activity, view)
                        popupMenu.inflate(menuRes)
                        popupMenu.setOnMenuItemClickListener { menuItem ->
                            if (menuItem != null)
                                return@setOnMenuItemClickListener dataset[0].menuHandler()
                                    ?.invoke(activity, dataset[bindingAdapterPosition - 1], menuItem.itemId) ?: false
                            else return@setOnMenuItemClickListener false
                        }
                        popupMenu.show()
                    }
                }
            }
        }
    }
    inner class DashboardHolder(itemView: View) : ViewHolder(itemView) {
        override val isItem: Boolean = false
    }
}

data class Dashboard(
    var name: String,
    var count: Int = 0,
    var duration: Long = 0,
    var path: String? = "",
)
