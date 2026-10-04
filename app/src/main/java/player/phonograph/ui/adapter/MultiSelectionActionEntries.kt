/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.mechanism.playlist.PlaylistSongsActions
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.file.FileItem
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.menu.ActionSemantic
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.Genres
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.actions.actionAddToPlaylist
import player.phonograph.ui.actions.actionDelete
import player.phonograph.ui.actions.actionDeletePlaylists
import player.phonograph.ui.actions.actionEnqueue
import player.phonograph.ui.actions.actionPlay
import player.phonograph.ui.actions.actionPlayNext
import player.phonograph.ui.actions.actionSavePlaylists
import player.phonograph.ui.actions.songs
import player.phonograph.ui.modules.tag.MultiTagBrowserActivity
import player.phonograph.ui.modules.tag.TagBrowserActivity
import player.phonograph.ui.resource.ActionSemantics
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            convertToSongs(it, context).actionPlay(ShuffleMode.NONE, 0)
        }

    fun <I> PlayNext(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.PlayNext, controller) {
            convertToSongs(it, context).actionPlayNext()
        }

    fun <I> ShuffleAll(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.ShuffleAll, controller) {
            val songs = convertToSongs(it, context)
            songs.actionPlay(ShuffleMode.SHUFFLE, Random().nextInt(songs.size))
        }

    fun <I> AddToPlayingQueue(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.AddToPlayingQueue, controller) {
            convertToSongs(it, context).actionEnqueue()
        }

    fun <I> AddToPlaylist(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.AddToPlaylist, controller) {
            convertToSongs(it, context).actionAddToPlaylist(context)
        }

    fun <I> TagEditor(context: Context, controller: MultiSelectionController<I>): ActionEntry =
        entry(ActionSemantics.TagEditor, controller) {
            val songs = convertToSongs(it, context)
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
                convertToSongs(it, context).actionDelete(context)
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

    private suspend fun convertToSongs(selections: Iterable<*>, context: Context): List<Song> =
        withContext(Dispatchers.Default) {
            selections.flatMap {
                when (it) {
                    is Song           -> listOf(it)
                    is QueueSong      -> listOf(it.song)
                    is Album          -> Songs.album(context, it.id)
                    is Artist         -> Songs.artist(context, it.id)
                    is Genre          -> Genres.songs(context, it.id)
                    is Playlist       -> PlaylistSongsActions.reader(it).allSongs(context)
                    is SongCollection -> it.songs
                    is FileItem       -> it.songs(context)
                    else              -> emptyList()
                }
            }
        }
}
