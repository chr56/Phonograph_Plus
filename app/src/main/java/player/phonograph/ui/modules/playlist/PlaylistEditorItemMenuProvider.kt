/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist

import player.phonograph.model.QueueSong
import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.actions.ActionMenuProviders.ActionMenuProvider
import player.phonograph.ui.actions.actionGotoDetail
import player.phonograph.ui.modules.auxiliary.DeletionDialog
import player.phonograph.ui.modules.tag.TagBrowserActivity
import player.phonograph.ui.resource.ActionSemantics
import player.phonograph.ui.util.inflateMenuEntries
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.view.Menu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


interface PlaylistEditorAdapterLinage {

    fun at(position: Int): QueueSong
    fun size(): Int

    suspend fun deleteSong(position: Int)
    suspend fun moveSong(from: Int, to: Int)
}

class PlaylistEditorItemMenuProvider(
    private val adapter: PlaylistEditorAdapterLinage,
) : ActionMenuProvider<QueueSong> {
    override fun inflateMenu(menu: Menu, context: Context, item: QueueSong, position: Int) {
        inflateMenuEntries(context, menu, editorItemMenu(context as FragmentActivity, position))
    }

    private fun editorItemMenu(activity: FragmentActivity, bindingAdapterPosition: Int): List<ActionEntry> {
        val queueSong = adapter.at(bindingAdapterPosition)
        return listOf(
            ActionEntry.ToolbarEntry(
                semantic = ActionSemantics.RemoveFromPlaylist,
                action = {
                    withContext(Dispatchers.IO) {
                        adapter.deleteSong(bindingAdapterPosition)
                    }
                }
            ),
            ActionEntry.ToolbarEntry(
                semantic = ActionSemantics.MoveToTop,
                action = {
                    withContext(Dispatchers.IO) {
                        adapter.moveSong(bindingAdapterPosition, 0)
                    }
                }
            ),
            ActionEntry.ToolbarEntry(
                semantic = ActionSemantics.MoveUp,
                action = {
                    if (bindingAdapterPosition != 0) {
                        withContext(Dispatchers.IO) {
                            adapter.moveSong(bindingAdapterPosition, bindingAdapterPosition - 1)
                        }
                    }
                }
            ),
            ActionEntry.ToolbarEntry(
                semantic = ActionSemantics.MoveDown,
                action = {
                    if (bindingAdapterPosition != adapter.size() - 1) {
                        withContext(Dispatchers.IO) {
                            adapter.moveSong(bindingAdapterPosition, bindingAdapterPosition + 1)
                        }
                    }
                }
            ),
            ActionEntry.ToolbarEntry(
                semantic = ActionSemantics.MoveToBottom,
                action = {
                    withContext(Dispatchers.IO) {
                        adapter.moveSong(bindingAdapterPosition, adapter.size() - 1)
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
}
