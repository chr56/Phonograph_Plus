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
import legacy.phonograph.LegacyPlaylistsUtil
import player.phonograph.R
import player.phonograph.adapter.base.AbsMultiSelectAdapter
import player.phonograph.adapter.base.MediaEntryViewHolder
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.helper.menu.SongMenuHelper
import player.phonograph.interfaces.CabHolder
import player.phonograph.mediastore.PlaylistSongLoader
import player.phonograph.model.PlaylistSong
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.util.MusicUtil
import player.phonograph.util.ViewUtil

class PlaylistEditorAdapter(
    val activity: AppCompatActivity,
    var playlist: Playlist,
    cabHolder: CabHolder
) : AbsMultiSelectAdapter<PlaylistEditorAdapter.ViewHolder, PlaylistSong>(activity, cabHolder, R.menu.menu_playlist_editor_selection),
    DraggableItemAdapter<PlaylistEditorAdapter.ViewHolder> {

    var playlistSongs: MutableList<PlaylistSong>

    init {
        playlistSongs = PlaylistSongLoader.getPlaylistSongList(activity, playlist.id).toMutableList()
        setHasStableIds(true)
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<PlaylistSong>) { // todo
        when (menuItem.itemId) {
            R.id.action_remove_from_playlist -> {
                selection.forEach {
                    playlistSongs.remove(it)
                    LegacyPlaylistsUtil.removeFromPlaylist(activity, it, playlist.id)
                }
                return
            }
            R.id.action_add_to_playlist -> {
                AddToPlaylistDialog.create(selection).show(
                    activity.supportFragmentManager, "ADD_TO_PLAYLIST"
                )
                return
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(activity).inflate(R.layout.item_list, parent, false)
        )
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = playlistSongs[position]

        val isChecked = isChecked(song)
        holder.itemView.isActivated = isChecked

        holder.title?.text = song.title
        holder.text?.text = MusicUtil.getSongInfoString(song)
        holder.shortSeparator?.visibility = View.VISIBLE

        holder.imageText?.visibility = View.VISIBLE
        holder.imageText?.text = (position + 1).toString()
        // todo add more items
        holder.menu?.setOnClickListener(
            object : SongMenuHelper.ClickMenuListener(activity, R.menu.menu_item_playlist_editor) {
                override val song: Song
                    get() = song

                override fun onMenuItemClick(item: MenuItem): Boolean {
                    val pos = playlistSongs.indexOf(song)
                    return when (item.itemId) {
                        R.id.action_remove_from_playlist -> {
                            LegacyPlaylistsUtil.removeFromPlaylist(activity, song, playlist.id)
                            playlistSongs.remove(song)
//                            notifyItemRemoved(pos)
                            notifyItemRangeChanged(pos, playlistSongs.size - 1) // so we can reorder the items behind removed one
                            true
                        }
                        R.id.action_move_to_top -> {
                            if (LegacyPlaylistsUtil.moveItem(activity, playlist.id, pos, 0)) {
                                playlistSongs.removeAt(pos)
                                playlistSongs.add(0, song)
                                notifyItemRangeChanged(0, pos + 1) // so we can reorder the items affected
//                                notifyDataSetChanged()
                            }
                            true
                        }
                        R.id.action_move_to_bottom -> {
                            if (LegacyPlaylistsUtil.moveItem(activity, playlist.id, pos, playlistSongs.size - 1)) {
                                playlistSongs.removeAt(pos)
                                playlistSongs.add(song)
                                notifyItemRangeChanged(pos - 1, playlistSongs.size - 1) // so we can reorder the items affected
//                                notifyDataSetChanged()
                            }
                            true
                        }
                        R.id.action_move_up -> {
                            if (pos == 0) return false
                            if (LegacyPlaylistsUtil.moveItem(activity, playlist.id, pos, pos - 1)) {
                                playlistSongs.removeAt(pos)
                                playlistSongs.add(pos - 1, song)
                                notifyItemRangeChanged(pos - 1, pos)
                            }
                            true
                        }
                        R.id.action_move_down -> {
                            if (pos == playlistSongs.size - 1) return false
                            if (LegacyPlaylistsUtil.moveItem(activity, playlist.id, pos, pos + 1)) {
                                playlistSongs.removeAt(pos)
                                playlistSongs.add(pos + 1, song)
                                notifyItemRangeChanged(pos, pos + 1)
                            }
                            true
                        }
                        else -> super.onMenuItemClick(item)
                    }
                }
            })
        holder.dragView?.visibility = View.VISIBLE
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean =
        position >= 0 &&
            (ViewUtil.hitTest(holder.dragView, x, y) || ViewUtil.hitTest(holder.imageText, x, y))

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange = ItemDraggableRange(0, playlistSongs.size - 1)

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition != toPosition) {
            if (LegacyPlaylistsUtil.moveItem(activity, playlist.id, fromPosition, toPosition)
            ) {
                // update dataset(playlistSongs)
                val newSongs = playlistSongs
                val song = newSongs.removeAt(fromPosition)
                newSongs.add(toPosition, song)
                playlistSongs = newSongs
            }
        }
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean = (dropPosition >= 0) && (dropPosition <= playlistSongs.size - 1)

    override fun onItemDragStarted(position: Int) {
    }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        when {
            fromPosition < toPosition -> notifyItemRangeChanged(fromPosition, toPosition)
            fromPosition > toPosition -> notifyItemRangeChanged(toPosition, fromPosition)
            fromPosition == toPosition -> notifyItemChanged(fromPosition)
        }

        // top position is kinda of special?
        if (fromPosition == 0) {
            notifyItemChanged(toPosition)
        }
        if (fromPosition == playlistSongs.size - 1 && toPosition == 0) {
            notifyItemChanged(playlistSongs.size - 1)
        }
    }

    override fun getItemId(position: Int): Long {
        return if (position < 0) -2 else playlistSongs[position].idInPlayList // todo
    }

    override fun getIdentifier(position: Int): PlaylistSong = playlistSongs[position]
    override fun getItemCount(): Int = playlistSongs.size

    inner class ViewHolder(itemView: View) :
        MediaEntryViewHolder(itemView),
        DraggableItemViewHolder {

        override fun onLongClick(v: View): Boolean {
            return toggleChecked(bindingAdapterPosition)
        }

        override fun onClick(v: View) {
            if (isInQuickSelectMode) {
                toggleChecked(bindingAdapterPosition)
            }
            return
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
