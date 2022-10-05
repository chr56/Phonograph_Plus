package player.phonograph.adapter

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity
import mt.util.color.resolveColor
import player.phonograph.R
import player.phonograph.actions.applyToPlaylistsToolbar
import player.phonograph.actions.injectPlaylistAdapter
import player.phonograph.adapter.base.MediaEntryViewHolder
import player.phonograph.adapter.base.MultiSelectAdapter
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.util.FavoriteUtil
import player.phonograph.util.NavigationUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class PlaylistAdapter(
    private val activity: FragmentActivity,
    dataSet: List<Playlist>,
    itemLayoutRes: Int?,
    cabController: MultiSelectionCabController?,
) : MultiSelectAdapter<PlaylistAdapter.ViewHolder, Playlist>(
    activity, cabController
) {

    @LayoutRes
    val itemLayoutRes: Int = itemLayoutRes ?: R.layout.item_list_single_row

    var dataSet = dataSet
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = dataSet[position].id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
        return createViewHolder(view, viewType)
    }

    private fun createViewHolder(view: View, viewType: Int): ViewHolder {
        return ViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = dataSet[position]
        holder.itemView.isActivated = isChecked(playlist)
        if (holder.title != null) {
            holder.title!!.text = playlist.name
        }
        if (holder.bindingAdapterPosition == itemCount - 1) {
            if (holder.shortSeparator != null) {
                holder.shortSeparator!!.visibility = View.GONE
            }
        } else {
            if (holder.shortSeparator != null && dataSet[position] !is SmartPlaylist) {
                holder.shortSeparator!!.visibility = View.VISIBLE
            }
        }
        if (holder.image != null) {
            holder.image!!.setImageResource(getIconRes(playlist))
        }
    }

    override fun updateItemCheckStatusForAll() = notifyDataSetChanged()

    override fun updateItemCheckStatus(datasetPosition: Int) = notifyItemChanged(datasetPosition)

    private fun getIconRes(playlist: Playlist): Int = when {
        playlist is SmartPlaylist -> playlist.iconRes
        FavoriteUtil.isFavoritePlaylist(activity, playlist) -> R.drawable.ic_favorite_white_24dp
        else -> R.drawable.ic_queue_music_white_24dp
    }

    override fun getItemViewType(position: Int): Int =
        if (dataSet[position] is SmartPlaylist) SMART_PLAYLIST else DEFAULT_PLAYLIST

    override fun getItemCount(): Int = dataSet.size

    override fun getItem(datasetPosition: Int): Playlist = dataSet[datasetPosition]

    override fun getName(obj: Playlist): String = obj.name


    override val multiSelectMenuHandler: ((Toolbar) -> Boolean)
        get() = {
            applyToPlaylistsToolbar(it.menu, activity, checkedList, cabTextColorColor) {
                checkAll()
                true
            }
        }
    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Playlist>) {}

    inner class ViewHolder(itemView: View, itemViewType: Int) : MediaEntryViewHolder(itemView) {
        init {
            if (itemViewType == SMART_PLAYLIST) {
                if (shortSeparator != null) {
                    shortSeparator!!.visibility = View.GONE
                }
                itemView.setBackgroundColor(resolveColor(activity, R.attr.cardBackgroundColor))
                // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                itemView.elevation =
                    activity.resources.getDimensionPixelSize(R.dimen.card_elevation).toFloat()
                // }
            }
            if (image != null) {
                val iconPadding =
                    activity.resources.getDimensionPixelSize(R.dimen.list_item_image_icon_padding)
                image!!.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                image!!.setColorFilter(
                    resolveColor(activity, R.attr.iconColor), PorterDuff.Mode.SRC_IN
                )
            }
            if (menu != null) {
                menu!!.setOnClickListener { view: View? ->
                    val playlist = dataSet[bindingAdapterPosition]
                    PopupMenu(activity, view).apply {
                        injectPlaylistAdapter(menu, activity, playlist)
                    }.show()
                }
            }
        }

        override fun onClick(v: View) {
            if (isInQuickSelectMode) {
                toggleChecked(bindingAdapterPosition)
            } else {
                NavigationUtil.goToPlaylist(activity, dataSet[bindingAdapterPosition])
            }
        }

        override fun onLongClick(v: View): Boolean {
            toggleChecked(bindingAdapterPosition)
            return true
        }
    }

    companion object {
        private const val SMART_PLAYLIST = 0
        private const val DEFAULT_PLAYLIST = 1
    }
}
