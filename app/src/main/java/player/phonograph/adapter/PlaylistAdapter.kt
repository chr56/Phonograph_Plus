package player.phonograph.adapter

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import util.mddesign.util.Util
import player.phonograph.R
import player.phonograph.adapter.base.MediaEntryViewHolder
import player.phonograph.adapter.base.MultiSelectAdapter
import player.phonograph.dialogs.ClearSmartPlaylistDialog
import player.phonograph.dialogs.DeletePlaylistDialog
import player.phonograph.helper.menu.PlaylistMenuHelper
import player.phonograph.helper.menu.SongsMenuHelper
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.model.AbsCustomPlaylist
import player.phonograph.model.Playlist
import player.phonograph.model.Song
import player.phonograph.model.smartplaylist.AbsSmartPlaylist
import player.phonograph.model.smartplaylist.LastAddedPlaylist
import player.phonograph.util.FavoriteUtil
import player.phonograph.util.NavigationUtil
import player.phonograph.util.SAFCallbackHandlerActivity
import util.phonograph.m3u.PlaylistsManager

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class PlaylistAdapter(
    private val activity: AppCompatActivity,
    dataSet: List<Playlist>,
    @param:LayoutRes private val itemLayoutRes: Int,
    cabProvider: MultiSelectionCabProvider?
) : MultiSelectAdapter<PlaylistAdapter.ViewHolder, Playlist>(
    activity, cabProvider
) {

    var dataSet = dataSet
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

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
            if (holder.shortSeparator != null && dataSet[position] !is AbsSmartPlaylist) {
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
        playlist is AbsSmartPlaylist -> playlist.iconRes
        FavoriteUtil.isFavoritePlaylist(activity, playlist) -> R.drawable.ic_favorite_white_24dp
        else -> R.drawable.ic_queue_music_white_24dp
    }

    override fun getItemViewType(position: Int): Int =
        if (dataSet[position] is AbsSmartPlaylist) SMART_PLAYLIST else DEFAULT_PLAYLIST

    override fun getItemCount(): Int = dataSet.size

    override fun getItem(datasetPosition: Int): Playlist = dataSet[datasetPosition]

    override fun getName(obj: Playlist): String = obj.name

    override var multiSelectMenuRes: Int = R.menu.menu_playlists_selection
    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Playlist>) {
        when (menuItem.itemId) {
            R.id.action_delete_playlist -> {
                val playlists: MutableList<Playlist> = selection as MutableList<Playlist>
                for (playlist in playlists) {
                    if (playlist != null) { // it shouldn't be, but we'd better do  null check
                        if (playlist is AbsSmartPlaylist) {
                            ClearSmartPlaylistDialog.create(playlist).show(
                                activity.supportFragmentManager,
                                "CLEAR_PLAYLIST_" + playlist.name
                            )
                            playlists.remove(playlist) // then remove this AbsSmartPlaylist
                        }
                    } else playlists.remove(playlist) // remove null playlist
                }
                // the rest should be "normal" playlists
                DeletePlaylistDialog.create(playlists as List<Playlist>)
                    .show(activity.supportFragmentManager, "DELETE_PLAYLIST")
            }
            R.id.action_save_playlist ->
                if (activity is SAFCallbackHandlerActivity) {
                    PlaylistsManager(activity, activity).duplicatePlaylistsViaSaf(selection)
                } else {
                    PlaylistsManager(activity, null).duplicatePlaylistsViaSaf(selection)
                }
            else ->
                // default, handle common items
                SongsMenuHelper.handleMenuClick(activity, getSongList(selection), menuItem.itemId)
        }
    }

    private fun getSongList(playlists: List<Playlist>): List<Song> {
        val songs: MutableList<Song> = ArrayList()
        for (playlist in playlists) {
            if (playlist is AbsCustomPlaylist) {
                songs.addAll(playlist.getSongs(activity))
            } else {
                songs.addAll(PlaylistSongLoader.getPlaylistSongList(activity, playlist.id))
            }
        }
        return songs
    }

    inner class ViewHolder(itemView: View, itemViewType: Int) : MediaEntryViewHolder(itemView) {
        init {
            if (itemViewType == SMART_PLAYLIST) {
                if (shortSeparator != null) {
                    shortSeparator!!.visibility = View.GONE
                }
                itemView.setBackgroundColor(Util.resolveColor(activity, R.attr.cardBackgroundColor))
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
                    Util.resolveColor(activity, R.attr.iconColor), PorterDuff.Mode.SRC_IN
                )
            }
            if (menu != null) {
                menu!!.setOnClickListener { view: View? ->
                    val playlist = dataSet[bindingAdapterPosition]
                    val popupMenu = PopupMenu(activity, view)
                    popupMenu.inflate(if (getItemViewType() == SMART_PLAYLIST) R.menu.menu_item_smart_playlist else R.menu.menu_item_playlist)
                    if (playlist is LastAddedPlaylist) {
                        popupMenu.menu.findItem(R.id.action_clear_playlist).isVisible = false
                    }
                    popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                        if (item.itemId == R.id.action_clear_playlist) {
                            if (playlist is AbsSmartPlaylist) {
                                ClearSmartPlaylistDialog.create(playlist).show(
                                    activity.supportFragmentManager,
                                    "CLEAR_SMART_PLAYLIST_" + playlist.name
                                )
                                return@setOnMenuItemClickListener true
                            }
                        }
                        PlaylistMenuHelper.handleMenuClick(
                            activity, dataSet[bindingAdapterPosition], item
                        )
                    }
                    popupMenu.show()
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
