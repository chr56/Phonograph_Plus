/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.song

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemState
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags
import player.phonograph.R
import player.phonograph.dialogs.RemoveFromPlaylistDialog
import player.phonograph.interfaces.CabHolder
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.model.Playlist
import player.phonograph.model.PlaylistSong
import player.phonograph.model.Song
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.ViewUtil

class PlaylistEditorAdapter(
    activity: AppCompatActivity,
    var playlist: Playlist,
    cabHolder: CabHolder,
//    private val onMoveItemListener: OnMoveItemListener
) : UniversalSongAdapter(activity, emptyList(), MODE_PLAYLIST_LOCAL, cabHolder),
    DraggableItemAdapter<PlaylistEditorAdapter.ViewHolder> {

    var playlistSongs: MutableList<PlaylistSong>

    init {
        setMultiSelectMenuRes(R.menu.menu_playlists_songs_selection)
        playlistSongs = PlaylistSongLoader.getPlaylistSongList(activity, playlist.id)
    }
    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        when (menuItem.itemId) {
            R.id.action_remove_from_playlist -> {
                RemoveFromPlaylistDialog.create(selection as List<PlaylistSong>).show(
                    activity.supportFragmentManager, "ADD_PLAYLIST"
                )
                return
            }
        }
        super.onMultipleItemAction(menuItem, selection)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonSongViewHolder {
        return ViewHolder(LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false))
    }

    override fun getItemId(position: Int): Long {
        return if (position <= 0) -2 else playlistSongs[position - 1].idInPlayList
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean =
        position > 0 &&
            (ViewUtil.hitTest(holder.dragView, x, y) || ViewUtil.hitTest(holder.image, x, y))

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange = ItemDraggableRange(1, songs.size)

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition != toPosition) {
//            onMoveItemListener.onMoveItem(fromPosition - 1, toPosition - 1)
            if (PlaylistsUtil.moveItem(activity, playlist.id, fromPosition - 1, toPosition - 1)
            ) {
                val songs = playlistSongs
                val song = songs.removeAt(fromPosition-1)
                songs.add(toPosition-1, song)
                playlistSongs = songs
            }
        }
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean = (dropPosition > 0)

    override fun onItemDragStarted(position: Int) { notifyDataSetChanged() }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) { notifyDataSetChanged() }

//    interface OnMoveItemListener {
//        fun onMoveItem(fromPosition: Int, toPosition: Int)
//    }

    inner class ViewHolder(itemView: View) :
        UniversalSongAdapter.CommonSongViewHolder(itemView),
        DraggableItemViewHolder {

        init {
            dragView?.visibility = View.VISIBLE
        }
        override val menuRes: Int
            get() = R.menu.menu_item_playlist_song_short // todo remove other items
        override fun onSongMenuItemClick(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_remove_from_playlist -> { // todo
                    RemoveFromPlaylistDialog.create(listOf(song as PlaylistSong))
                        .show(activity.supportFragmentManager, "REMOVE_FROM_PLAYLIST")
                    return true
                }
            }
            return super.onSongMenuItemClick(item)
        }

        @DraggableItemStateFlags
        private var dragStateFlags = 0
        override fun setDragStateFlags(flags: Int) { dragStateFlags = flags }
        override fun getDragStateFlags(): Int = dragStateFlags
        override fun getDragState(): DraggableItemState = DraggableItemState().apply {
            this.flags = dragStateFlags
        }
    }
}
