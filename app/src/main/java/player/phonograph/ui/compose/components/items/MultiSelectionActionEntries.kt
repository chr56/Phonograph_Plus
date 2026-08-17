/*
 * Copyright (c) 2026 chr_56
 */

package player.phonograph.ui.compose.components.items

import player.phonograph.model.Song
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
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("FunctionName")
object MultiSelectionActionEntries {

    fun <T> entries(
        context: Context,
        state: MultiSelectionState<T>,
        items: List<T>,
        value: (T) -> Any? = { it },
    ): List<ActionEntry> = buildList {
        add(Play(context, state, value = value))
        add(PlayNext(context, state, value = value))
        add(ShuffleAll(context, state, value = value))
        add(AddToPlayingQueue(context, state, value = value))
        add(AddToPlaylist(context, state, value = value))
        add(TagEditor(context, state, value = value))
        add(DeleteFromDevice(context, state, value = value))
        if (state.selected.any { value(it) is Playlist }) {
            add(SavePlaylists(context, state, value = value))
        }
        add(SelectAll(state, items))
        add(UnselectAll(state))
    }

    private fun <T> entry(
        semantic: ActionSemantic,
        state: MultiSelectionState<T>,
        value: (T) -> Any?,
        visibility: Int,
        dangerous: Boolean = false,
        action: suspend (List<Any?>) -> Unit,
    ): ActionEntry.ToolbarEntry = ActionEntry.ToolbarEntry(
        semantic = semantic,
        visibility = visibility,
        dangerous = dangerous,
        action = {
            withContext(Dispatchers.Main.immediate) {
                val selected = state.selected
                if (selected.isNotEmpty()) {
                    action(selected.map(value))
                    state.clear()
                }
            }
        },
    )

    private suspend fun songs(context: Context, items: List<Any?>): List<Song> =
        withContext(Dispatchers.IO) { convertToSongs(context, items) }

    private fun <T> songEntry(
        context: Context,
        semantic: ActionSemantic,
        state: MultiSelectionState<T>,
        value: (T) -> Any?,
        visibility: Int,
        action: suspend (List<Song>) -> Unit,
    ): ActionEntry.ToolbarEntry =
        entry(semantic, state, value, visibility) {
            val songs = songs(context, it)
            if (songs.isNotEmpty()) action(songs)
        }

    fun <T> Play(
        context: Context,
        state: MultiSelectionState<T>,
        visibility: Int = ActionEntry.ToolbarEntry.ALWAYS,
        value: (T) -> Any? = { it },
    ): ActionEntry.ToolbarEntry =
        songEntry(context, ActionSemantics.Play, state, value, visibility) {
            it.actionPlay(ShuffleMode.NONE, 0)
        }

    fun <T> PlayNext(
        context: Context,
        state: MultiSelectionState<T>,
        visibility: Int = ActionEntry.ToolbarEntry.IF_ROOM,
        value: (T) -> Any? = { it },
    ): ActionEntry.ToolbarEntry =
        songEntry(context, ActionSemantics.PlayNext, state, value, visibility) {
            it.actionPlayNext()
        }

    fun <T> ShuffleAll(
        context: Context,
        state: MultiSelectionState<T>,
        visibility: Int = ActionEntry.ToolbarEntry.IF_ROOM,
        value: (T) -> Any? = { it },
    ): ActionEntry.ToolbarEntry =
        songEntry(context, ActionSemantics.ShuffleAll, state, value, visibility) {
            it.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(it.size))
        }

    fun <T> AddToPlayingQueue(
        context: Context,
        state: MultiSelectionState<T>,
        visibility: Int = ActionEntry.ToolbarEntry.ALWAYS,
        value: (T) -> Any? = { it },
    ): ActionEntry.ToolbarEntry =
        songEntry(context, ActionSemantics.AddToPlayingQueue, state, value, visibility) {
            it.actionEnqueue()
        }

    fun <T> AddToPlaylist(
        context: Context,
        state: MultiSelectionState<T>,
        visibility: Int = ActionEntry.ToolbarEntry.IF_ROOM,
        value: (T) -> Any? = { it },
    ): ActionEntry.ToolbarEntry =
        songEntry(context, ActionSemantics.AddToPlaylist, state, value, visibility) {
            it.actionAddToPlaylist(context)
        }

    fun <T> TagEditor(
        context: Context,
        state: MultiSelectionState<T>,
        visibility: Int = ActionEntry.ToolbarEntry.IF_ROOM,
        value: (T) -> Any? = { it },
    ): ActionEntry.ToolbarEntry =
        songEntry(context, ActionSemantics.TagEditor, state, value, visibility) {
            if (it.size > 1) {
                MultiTagBrowserActivity.launch(context, ArrayList(it.map { song -> song.data }))
            } else {
                TagBrowserActivity.launch(context, it.first().data)
            }
        }

    fun <T> DeleteFromDevice(
        context: Context,
        state: MultiSelectionState<T>,
        visibility: Int = ActionEntry.ToolbarEntry.IF_ROOM,
        value: (T) -> Any? = { it },
    ): ActionEntry.ToolbarEntry =
        entry(ActionSemantics.DeleteFromDevice, state, value, visibility, dangerous = true) {
            val playlists = it.filterIsInstance<Playlist>()
            // Delete playlists instead of their songs when playlists are selected.
            if (playlists.isNotEmpty()) {
                playlists.actionDeletePlaylists(context)
            } else {
                val songs = songs(context, it)
                if (songs.isNotEmpty()) songs.actionDelete(context)
            }
        }

    fun <T> SavePlaylists(
        context: Context,
        state: MultiSelectionState<T>,
        visibility: Int = ActionEntry.ToolbarEntry.IF_ROOM,
        value: (T) -> Any? = { it },
    ): ActionEntry.ToolbarEntry =
        entry(ActionSemantics.SavePlaylists, state, value, visibility) {
            val playlists = it.filterIsInstance<Playlist>()
            if (playlists.isNotEmpty()) playlists.actionSavePlaylists(context)
        }

    fun <T> SelectAll(
        state: MultiSelectionState<T>,
        items: List<T>,
        visibility: Int = ActionEntry.ToolbarEntry.IF_ROOM,
    ): ActionEntry.ToolbarEntry =
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.SelectAll,
            visibility = visibility,
            action = { state.select(items) },
        )

    fun <T> UnselectAll(
        state: MultiSelectionState<T>,
        visibility: Int = ActionEntry.ToolbarEntry.NEVER,
    ): ActionEntry.ToolbarEntry =
        ActionEntry.ToolbarEntry(
            semantic = ActionSemantics.UnselectAll,
            visibility = visibility,
            action = { state.clear() },
        )
}
