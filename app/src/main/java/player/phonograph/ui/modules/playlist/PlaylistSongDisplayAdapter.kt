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
import kotlinx.coroutines.launch

class PlaylistSongDisplayAdapter(
    activity: FragmentActivity,
    val viewModel: PlaylistDetailViewModel,
) : DraggableDisplayAdapter<QueueSong>(
    activity,
    PlaylistSongDisplayPresenter(viewModel),
    DisplayOptions(
        ItemLayoutStyle.LIST, null, false, imageryType = DisplayOptions.ORDINAL_TEXT
    )
) {

    fun clearSelection() {
        if (controller.selected.isNotEmpty()) controller.unselectedAll()
    }

    class PlaylistSongDisplayPresenter(
        private val viewModel: PlaylistDetailViewModel,
    ) : QueueSongDisplayPresenter() {
        override fun getSectionText(item: QueueSong, sortMode: SortMode?): String = (item.index + 1).toString()
        override fun getOrdinalText(item: QueueSong, position: Int): String = (item.index + 1).toString()

        override fun clickAction(context: Context, item: QueueSong, position: Int, list: List<QueueSong>): ActionEntry =
            ItemClickActions.Song(context, item.song, position, list.map { it.song })

        override fun menuEntries(context: Context, item: QueueSong, position: Int): List<ActionEntry> =
            if (viewModel.currentMode.value == UIMode.Editor) {
                playlistEditorItemMenuEntries(context as FragmentActivity, item, position, viewModel)
            } else {
                ActionMenus.Song(context, item.song, showPlay = false)
            }
    }

    override fun onBindViewHolder(holder: DisplayViewHolder<QueueSong>, position: Int) {
        holder.dragView?.visibility = if (editableMode()) View.VISIBLE else View.GONE
        super.onBindViewHolder(holder, position)
    }

    //region Draggable
    override fun editableMode(): Boolean = viewModel.currentMode.value == UIMode.Editor

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        activity.lifecycleScope.launch {
            viewModel.execute(activity, EditAction.Move(fromPosition, toPosition))
        }
    }
    //endregion
}
