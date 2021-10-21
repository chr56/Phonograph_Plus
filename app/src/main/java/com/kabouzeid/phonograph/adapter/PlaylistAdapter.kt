package com.kabouzeid.phonograph.adapter

import android.content.Context
import android.graphics.PorterDuff
import android.os.Build
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import chr_56.MDthemer.util.Util
import com.kabouzeid.phonograph.App
import com.kabouzeid.phonograph.R
import com.kabouzeid.phonograph.adapter.base.AbsMultiSelectAdapter
import com.kabouzeid.phonograph.adapter.base.MediaEntryViewHolder
import com.kabouzeid.phonograph.dialogs.ClearSmartPlaylistDialog
import com.kabouzeid.phonograph.dialogs.DeletePlaylistDialog
import com.kabouzeid.phonograph.helper.menu.PlaylistMenuHelper.handleMenuClick
import com.kabouzeid.phonograph.helper.menu.SongsMenuHelper
import com.kabouzeid.phonograph.interfaces.CabHolder
import com.kabouzeid.phonograph.loader.PlaylistSongLoader
import com.kabouzeid.phonograph.misc.WeakContextAsyncTask
import com.kabouzeid.phonograph.model.AbsCustomPlaylist
import com.kabouzeid.phonograph.model.Playlist
import com.kabouzeid.phonograph.model.Song
import com.kabouzeid.phonograph.model.smartplaylist.AbsSmartPlaylist
import com.kabouzeid.phonograph.model.smartplaylist.LastAddedPlaylist
import com.kabouzeid.phonograph.util.MusicUtil
import com.kabouzeid.phonograph.util.NavigationUtil
import com.kabouzeid.phonograph.util.PlaylistsUtil
import java.io.IOException

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class PlaylistAdapter(
    private val activity: AppCompatActivity,
    var dataSet: List<Playlist>,
    @param:LayoutRes private val itemLayoutRes: Int,
    cabHolder: CabHolder?
) : AbsMultiSelectAdapter<PlaylistAdapter.ViewHolder, Playlist>(
    activity, cabHolder, R.menu.menu_playlists_selection
) {

    init {
        setHasStableIds(true)
    }

    fun swapDataSet(dataSet: List<Playlist>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
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

    private fun getIconRes(playlist: Playlist): Int {
        return if (playlist is AbsSmartPlaylist)
            playlist.iconRes
        else if (MusicUtil.isFavoritePlaylist(activity, playlist))
            R.drawable.ic_favorite_white_24dp
        else R.drawable.ic_queue_music_white_24dp
    }

    override fun getItemViewType(position: Int): Int {
        return if (dataSet[position] is AbsSmartPlaylist) SMART_PLAYLIST else DEFAULT_PLAYLIST
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getIdentifier(position: Int): Playlist {
        return dataSet[position]
    }

    override fun getName(obj: Playlist): String {
        return obj.name
    }

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
                if (selection.size == 1) {
                    handleMenuClick(activity, selection[0], menuItem)
                } else {
                    SavePlaylistsAsyncTask(activity).execute(selection)
                }
            else ->
                // default, handle common items
                SongsMenuHelper.handleMenuClick(activity, getSongList(selection), menuItem.itemId)
        }
    }

    private class SavePlaylistsAsyncTask(context: Context?) :
        WeakContextAsyncTask<List<Playlist?>?, String?, String?>(context) {
        override fun doInBackground(vararg params: List<Playlist?>?): String {
            var successes = 0
            var failures = 0
            var dir: String? = ""
            for (playlist in params[0]!!) {
                try {
                    dir = PlaylistsUtil.savePlaylist(
                        App.instance.applicationContext,
                        playlist
                    ).parent
                    successes++
                } catch (e: IOException) {
                    failures++
                    e.printStackTrace()
                }
            }
            return if (failures == 0) String.format(
                App.instance.applicationContext.getString(R.string.saved_x_playlists_to_x),
                successes, dir
            ) else String.format(
                App.instance.applicationContext.getString(R.string.saved_x_playlists_to_x_failed_to_save_x),
                successes, dir, failures
            )
        }

        override fun onPostExecute(string: String?) {
            super.onPostExecute(string)
            val context = context
            if (context != null) {
                Toast.makeText(context, string, Toast.LENGTH_LONG).show()
            }
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    itemView.elevation =
                        activity.resources.getDimensionPixelSize(R.dimen.card_elevation).toFloat()
                }
            }
            if (image != null) {
                val iconPadding = activity.resources.getDimensionPixelSize(R.dimen.list_item_image_icon_padding)
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
                        handleMenuClick(
                            activity, dataSet[bindingAdapterPosition], item
                        )
                    }
                    popupMenu.show()
                }
            }
        }
        override fun onClick(view: View) {
            if (isInQuickSelectMode) {
                toggleChecked(bindingAdapterPosition)
            } else {
                val playlist = dataSet[bindingAdapterPosition]
                NavigationUtil.goToPlaylist(activity, playlist)
            }
        }

        override fun onLongClick(view: View): Boolean {
            toggleChecked(bindingAdapterPosition)
            return true
        }
    }

    companion object {
        private const val SMART_PLAYLIST = 0
        private const val DEFAULT_PLAYLIST = 1
    }
}
