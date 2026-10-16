/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.playlist

import player.phonograph.model.QueueSong
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.sort.SortMode
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.model.ui.UIMode
import player.phonograph.ui.actions.ActionMenus
import player.phonograph.ui.actions.ItemClickActions
import player.phonograph.ui.adapter.DisplayOptions
import player.phonograph.ui.adapter.DraggableDisplayAdapter
import player.phonograph.ui.adapter.QueueSongDisplayPresenter
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistSongDisplayAdapter(
    activity: FragmentActivity,
    val viewModel: PlaylistDetailViewModel,
    presenter: PlaylistSongDisplayPresenter,
) : DraggableDisplayAdapter<QueueSong>(
    activity, presenter, DisplayOptions(ItemLayoutStyle.LIST, null, false)
) {

    class PlaylistSongDisplayPresenter(
        val accessMenuEntries: (Context, QueueSong, Int) -> List<ActionEntry>,
    ) : QueueSongDisplayPresenter() {
        override fun getSectionText(item: QueueSong, sortMode: SortMode?): String = (item.index + 1).toString()
        override fun getOrdinalText(item: QueueSong, position: Int): String = (item.index + 1).toString()

        override fun clickAction(context: Context, item: QueueSong, position: Int, list: List<QueueSong>): ActionEntry? =
            ItemClickActions.Song(context, item.song, position, list.map { it.song })

        override fun menuEntries(context: Context, item: QueueSong, position: Int): List<ActionEntry> =
            accessMenuEntries(context, item, position)
    }

    fun menuEntries(context: Context, item: QueueSong, position: Int): List<ActionEntry> =
        if (viewModel.currentMode.value == UIMode.Editor) {
            playlistEditorItemMenuEntries(context as FragmentActivity, position, adapterLinage)
        } else {
            ActionMenus.Song(context, item.song, showPlay = false)
        }

    private val adapterLinage = object : PlaylistEditorAdapterConnection {
        override fun at(position: Int): QueueSong = dataset[position]

        override fun size(): Int = dataset.size

        override suspend fun deleteSong(position: Int) =
            this@PlaylistSongDisplayAdapter.deleteSong(position)

        override suspend fun moveSong(from: Int, to: Int) =
            this@PlaylistSongDisplayAdapter.moveSong(from, to)
    }

    override fun onBindViewHolder(holder: DisplayViewHolder<QueueSong>, position: Int) {
        if (viewModel.currentMode.value == UIMode.Editor) {
            holder.dragView?.visibility = View.VISIBLE
        }
        super.onBindViewHolder(holder, position)
    }

    //region Logic
    private suspend fun moveSong(fromPosition: Int, toPosition: Int) {
        val result = withContext(Dispatchers.IO) {
            viewModel.moveItem(activity, fromPosition, toPosition)
        }
        if (result) withContext(Dispatchers.Main) {
            synchronized(dataset) {
                val newSongs: MutableList<QueueSong> = dataset.toMutableList()
                val song = newSongs.removeAt(fromPosition)
                newSongs.add(toPosition, song)
                dataset = newSongs
            }
        }
    }

    private suspend fun deleteSong(position: Int) {
        val queueSong = dataset[position]
        val result = withContext(Dispatchers.IO) {
            viewModel.deleteItem(activity, queueSong.song, position)
        }
        if (result) withContext(Dispatchers.Main) {
            synchronized(dataset) {
                dataset = dataset.toMutableList().also { it.removeAt(position) }
            }
            notifyItemRangeChanged(position, dataset.size - 1)
        }
    }
    //endregion


    //region Draggable
    override fun editableMode(): Boolean = viewModel.currentMode.value == UIMode.Editor

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        activity.lifecycleScope.launch(Dispatchers.IO) { moveSong(fromPosition, toPosition) }
    }
    //endregion
}
