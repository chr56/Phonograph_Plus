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

fun playlistEditorItemMenuEntries(
    activity: FragmentActivity,
    queueSong: QueueSong,
    position: Int,
    viewModel: PlaylistDetailViewModel,
): List<ActionEntry> {
    return listOf(
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.RemoveFromPlaylist,
            action = {
                viewModel.execute(activity, PlaylistAction.Edit.Delete(queueSong.song, position))
            }
        ),
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.MoveToTop,
            action = {
                viewModel.execute(activity, PlaylistAction.Edit.Move(position, 0))
            }
        ),
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.MoveUp,
            action = {
                if (position != 0) {
                    viewModel.execute(activity, PlaylistAction.Edit.Move(position, position - 1))
                }
            }
        ),
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.MoveDown,
            action = {
                if (position != viewModel.songs.value.lastIndex) {
                    viewModel.execute(activity, PlaylistAction.Edit.Move(position, position + 1))
                }
            }
        ),
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.MoveToBottom,
            action = {
                viewModel.execute(activity, PlaylistAction.Edit.Move(position, viewModel.songs.value.lastIndex))
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
