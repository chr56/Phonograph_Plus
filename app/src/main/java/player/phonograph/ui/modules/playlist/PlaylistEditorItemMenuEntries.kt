/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.playlist

import player.phonograph.model.QueueSong
import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.actions.actionGotoDetail
import player.phonograph.ui.modules.auxiliary.DeletionDialog
import player.phonograph.ui.modules.tag.TagBrowserActivity
import player.phonograph.ui.resource.ActionSemantics
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


interface PlaylistEditorAdapterConnection {

    fun at(position: Int): QueueSong
    fun size(): Int

    suspend fun deleteSong(position: Int)
    suspend fun moveSong(from: Int, to: Int)
}

fun playlistEditorItemMenuEntries(
    activity: FragmentActivity,
    position: Int,
    adapter: PlaylistEditorAdapterConnection,
): List<ActionEntry> {
    val queueSong = adapter.at(position)
    return listOf(
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.RemoveFromPlaylist,
            action = {
                withContext(Dispatchers.IO) {
                    adapter.deleteSong(position)
                }
            }
        ),
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.MoveToTop,
            action = {
                withContext(Dispatchers.IO) {
                    adapter.moveSong(position, 0)
                }
            }
        ),
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.MoveUp,
            action = {
                if (position != 0) {
                    withContext(Dispatchers.IO) {
                        adapter.moveSong(position, position - 1)
                    }
                }
            }
        ),
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.MoveDown,
            action = {
                if (position != adapter.size() - 1) {
                    withContext(Dispatchers.IO) {
                        adapter.moveSong(position, position + 1)
                    }
                }
            }
        ),
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.MoveToBottom,
            action = {
                withContext(Dispatchers.IO) {
                    adapter.moveSong(position, adapter.size() - 1)
                }
            }
        ),
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.Details,
            action = { queueSong.song.actionGotoDetail(activity) }
        ),
        ActionEntry.GroupedEntries(
            semantic = ActionSemantics.More,
            entries = listOf(
                ActionEntry.ToolbarEntry(
                    semantic = ActionSemantics.TagEditor,
                    action = { TagBrowserActivity.launch(activity, queueSong.song.data) }
                ),
                ActionEntry.ToolbarEntry(
                    semantic = ActionSemantics.DeleteFromDevice,
                    action = {
                        DeletionDialog.create(arrayListOf(queueSong.song))
                            .show(activity.supportFragmentManager, "DELETE_SONGS")
                    }
                )
            )
        )
    )
}
