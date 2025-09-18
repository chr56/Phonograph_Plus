/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.playlist

import player.phonograph.model.QueueSong
import player.phonograph.model.ui.UIMode
import player.phonograph.ui.actions.ActionMenuProviders
import player.phonograph.ui.actions.ClickActionProviders
import player.phonograph.ui.adapter.DraggableDisplayAdapter
import player.phonograph.ui.adapter.QueueSongBasicDisplayPresenter
import player.phonograph.util.produceSafeId
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.view.Menu
import android.view.View
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistSongDisplayAdapter(
    activity: FragmentActivity,
    val viewModel: PlaylistDetailViewModel,
    presenter: PlaylistSongDisplayPresenter,
) : DraggableDisplayAdapter<QueueSong>(activity, presenter) {

    class PlaylistSongDisplayPresenter(
        val accessMenuProvider: () -> ActionMenuProviders.ActionMenuProvider<QueueSong>?,
    ) : QueueSongBasicDisplayPresenter() {
        override fun getNonSortOrderReference(item: QueueSong): String? = (item.index + 1).toString()
        override fun getRelativeOrdinalText(item: QueueSong): String? = (item.index + 1).toString()

        override val clickActionProvider: ClickActionProviders.ClickActionProvider<QueueSong> =
            object : ClickActionProviders.ClickActionProvider<QueueSong> {
                override fun listClick(
                    list: List<QueueSong>, position: Int, context: Context, imageView: ImageView?,
                ): Boolean {
                    return ClickActionProviders.SongClickActionProvider()
                        .listClick(list.map { it.song }, position, context, imageView)
                }

            }
        override val menuProvider: ActionMenuProviders.ActionMenuProvider<QueueSong>? get() = accessMenuProvider()
    }

    val menuProvider: ActionMenuProviders.ActionMenuProvider<QueueSong> =
        object : ActionMenuProviders.ActionMenuProvider<QueueSong> {
            override fun inflateMenu(menu: Menu, context: Context, item: QueueSong, position: Int) {}
            override fun prepareMenu(menuButtonView: View, item: QueueSong, bindingPosition: Int) {
                if (viewModel.currentMode.value == UIMode.Editor) {
                    PlaylistEditorItemMenuProvider(adapterLinage)
                        .prepareMenu(menuButtonView, item, bindingPosition)
                } else {
                    ActionMenuProviders.SongActionMenuProvider(showPlay = false)
                        .prepareMenu(menuButtonView, item.song, bindingPosition)
                }
            }

            private val adapterLinage = object : PlaylistEditorAdapterLinage {
                override fun at(position: Int): QueueSong = dataset[position]

                override fun size(): Int = dataset.size

                override suspend fun deleteSong(position: Int) =
                    this@PlaylistSongDisplayAdapter.deleteSong(position)

                override suspend fun moveSong(from: Int, to: Int) =
                    this@PlaylistSongDisplayAdapter.moveSong(from, to)

            }
        }

    override fun getItemId(position: Int): Long = produceSafeId(presenter.getItemID(dataset[position]), position)

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
