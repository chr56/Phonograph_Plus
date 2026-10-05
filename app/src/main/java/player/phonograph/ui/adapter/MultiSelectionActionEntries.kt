/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.menu.ActionSemantic
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.service.ShuffleMode
import player.phonograph.ui.actions.actionAddToPlaylist
import player.phonograph.ui.actions.actionDelete
import player.phonograph.ui.actions.actionDeletePlaylists
import player.phonograph.ui.actions.actionEnqueue
import player.phonograph.ui.actions.actionPlay
import player.phonograph.ui.actions.actionPlayNext
import player.phonograph.ui.actions.actionSavePlaylists
import player.phonograph.ui.modules.tag.MultiTagBrowserActivity
import player.phonograph.ui.modules.tag.TagBrowserActivity
import player.phonograph.ui.resource.ActionSemantics
import player.phonograph.ui.util.convertToSongs
import android.content.Context
import java.util.Random

@Suppress("FunctionName", "UNUSED_PARAMETER")
object MultiSelectionActionEntries {

    private fun <I> entry(
        semantic: ActionSemantic,
        controller: MultiSelectionController<I>,
        visibility: Int = ActionEntry.ToolbarEntry.IF_ROOM,
        dangerous: Boolean = false,
        action: suspend (List<I>) -> Unit,
    ): ActionEntry = ActionEntry.ToolbarEntry(
        semantic = semantic,
        visibility = visibility,
        dangerous = dangerous,
        action = {
            action(controller.selected)
            controller.unselectedAll()
        },
    )

    fun <I> Play(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.Play, controller) {
            convertToSongs(context, it).actionPlay(ShuffleMode.NONE, 0)
        }

    fun <I> PlayNext(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.PlayNext, controller) {
            convertToSongs(context, it).actionPlayNext()
        }

    fun <I> ShuffleAll(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.ShuffleAll, controller) {
            val songs = convertToSongs(context, it)
            songs.actionPlay(ShuffleMode.SHUFFLE, Random().nextInt(songs.size))
        }

    fun <I> AddToPlayingQueue(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.AddToPlayingQueue, controller) {
            convertToSongs(context, it).actionEnqueue()
        }

    fun <I> AddToPlaylist(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.AddToPlaylist, controller) {
            convertToSongs(context, it).actionAddToPlaylist(context)
        }

    fun <I> TagEditor(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.TagEditor, controller) {
            val songs = convertToSongs(context, it)
            if (songs.size > 1) {
                MultiTagBrowserActivity.launch(context, ArrayList(songs.map { song -> song.data }))
            } else if (songs.size == 1) {
                TagBrowserActivity.launch(context, songs.first().data)
            }
        }

    fun <I> DeleteFromDevice(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.DeleteFromDevice, controller, dangerous = true) {
            val playlists = it.filterIsInstance<Playlist>()
            // Delete playlists instead of their songs when playlists are selected.
            if (playlists.isEmpty()) {
                convertToSongs(context, it).actionDelete(context)
            } else {
                playlists.actionDeletePlaylists(context)
            }
        }

    fun <I> SavePlaylists(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.SavePlaylists, controller) {
            it.filterIsInstance<Playlist>().actionSavePlaylists(context)
        }

    fun <I> SelectAll(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.SelectAll,
            visibility = ActionEntry.ToolbarEntry.IF_ROOM,
            action = { controller.selectAll() },
        )

    fun <I> InvertSelection(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.InvertSelection,
            visibility = ActionEntry.ToolbarEntry.NEVER,
            action = { controller.invertSelected() },
        )

    fun <I> UnselectAll(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.UnselectAll,
            visibility = ActionEntry.ToolbarEntry.NEVER,
            action = { controller.unselectedAll() },
        )
}
