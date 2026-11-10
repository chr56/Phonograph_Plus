/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.menu.ActionSemantic
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.service.ShuffleMode
import player.phonograph.ui.actions.ItemActions
import player.phonograph.ui.goToSongDetail
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
            ItemActions.play(context, convertToSongs(context, it), ShuffleMode.NONE, 0)
        }

    fun <I> PlayNext(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.PlayNext, controller) {
            ItemActions.playNext(context, convertToSongs(context, it))
        }

    fun <I> ShuffleAll(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.ShuffleAll, controller) {
            val songs = convertToSongs(context, it)
            ItemActions.play(context, songs, ShuffleMode.SHUFFLE, Random().nextInt(songs.size))
        }

    fun <I> AddToPlayingQueue(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.AddToPlayingQueue, controller) {
            ItemActions.enqueue(context, convertToSongs(context, it))
        }

    fun <I> AddToPlaylist(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.AddToPlaylist, controller) {
            ItemActions.addToPlaylist(context, convertToSongs(context, it))
        }

    fun <I> TagEditor(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.TagEditor, controller) {
            val songs = convertToSongs(context, it)
            if (songs.size > 1) {
                goToSongDetail(context, ArrayList(songs.map { song -> song.data }))
            } else if (songs.size == 1) {
                goToSongDetail(context, songs.first().data)
            }
        }

    fun <I> DeleteFromDevice(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.DeleteFromDevice, controller, dangerous = true) {
            val playlists = it.filterIsInstance<Playlist>()
            // Delete playlists instead of their songs when playlists are selected.
            if (playlists.isEmpty()) {
                ItemActions.delete(context, convertToSongs(context, it))
            } else {
                ItemActions.deletePlaylists(context, playlists)
            }
        }

    fun <I> SavePlaylists(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.SavePlaylists, controller) {
            ItemActions.savePlaylists(context, it.filterIsInstance<Playlist>())
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
