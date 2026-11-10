/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.actions

import player.phonograph.R
import player.phonograph.foundation.notification.ProgressNotificationConnection
import player.phonograph.mechanism.event.EventHub
import player.phonograph.mechanism.scanner.MediaStoreScanner
import player.phonograph.model.Song
import player.phonograph.model.file.FileItem
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.menu.ActionSemantic
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_BACKGROUND_TASKS
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.PinedPlaylists
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import player.phonograph.ui.goToSongDetail
import player.phonograph.ui.resource.ActionSemantics
import player.phonograph.ui.util.convertToSong
import player.phonograph.ui.util.convertToSongs
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

@Suppress("FunctionName", "UNUSED_PARAMETER")
object ActionEntries {

    private fun entry(
        semantic: ActionSemantic,
        visibility: Int = ActionEntry.ToolbarEntry.NEVER,
        dangerous: Boolean = false,
        action: suspend () -> Unit,
    ): ActionEntry = ActionEntry.ToolbarEntry(semantic, action, visibility, dangerous)

    fun More(context: Context, entries: List<ActionEntry>): ActionEntry =
        ActionEntry.GroupedEntries(ActionSemantics.More, entries)

    //region Song
    fun Play(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.Play) { ItemActions.play(context, song) }

    fun PlayNext(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.PlayNext) { ItemActions.playNext(context, song) }

    fun AddToPlayingQueue(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.AddToPlayingQueue) { ItemActions.enqueue(context, song) }

    fun RemoveFromPlayingQueue(context: Context, song: Song, index: Int): ActionEntry =
        entry(ActionSemantics.RemoveFromPlayingQueue) {
            MusicPlayerRemote.queueManager.removeSongAt(index)
        }

    fun AddToPlaylist(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.AddToPlaylist) {
            ItemActions.addToPlaylist(context, listOf(song))
        }

    fun GoToAlbum(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.GoToAlbum) {
            ItemActions.gotoAlbum(context, song)
        }

    fun GoToArtist(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.GoToArtist) {
            ItemActions.gotoArtist(context, song)
        }

    fun Details(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.Details) {
            withActivity(context) { ItemActions.gotoDetail(it, song) }
        }

    fun Share(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.Share) { ItemActions.share(context, song) }

    fun TagEditor(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.TagEditor) { ItemActions.tagEditor(context, song) }

    fun SetAsRingtone(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.SetAsRingtone) { ItemActions.setAsRingtone(context, song) }

    fun AddToBlackList(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.AddToBlackList) {
            ItemActions.addToBlacklist(context, song)
        }

    fun DeleteFromDevice(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.DeleteFromDevice, dangerous = true) {
            ItemActions.delete(context, listOf(song))
        }
    //endregion

    //region Composite
    fun <I> Play(context: Context, item: I, readSongs: suspend (Context, I) -> List<Song>): ActionEntry =
        entry(ActionSemantics.Play) {
            ItemActions.play(context, readSongs(context, item), ShuffleMode.NONE, 0)
        }

    fun <I> PlayNext(context: Context, item: I, readSongs: suspend (Context, I) -> List<Song>): ActionEntry =
        entry(ActionSemantics.PlayNext) {
            ItemActions.playNext(context, readSongs(context, item))
        }

    fun <I> AddToPlayingQueue(context: Context, item: I, readSongs: suspend (Context, I) -> List<Song>): ActionEntry =
        entry(ActionSemantics.AddToPlayingQueue) {
            ItemActions.enqueue(context, readSongs(context, item))
        }

    fun <I> AddToPlaylist(context: Context, item: I, readSongs: suspend (Context, I) -> List<Song>): ActionEntry =
        entry(ActionSemantics.AddToPlaylist) {
            withActivity(context) { ItemActions.addToPlaylist(it, readSongs(context, item)) }
        }

    fun <I> DeleteFromDevice(context: Context, item: I, readSongs: suspend (Context, I) -> List<Song>): ActionEntry =
        entry(ActionSemantics.DeleteFromDevice, dangerous = true) {
            withActivity(context) { ItemActions.delete(it, readSongs(context, item)) }
        }
    //endregion

    //region Playlist
    fun Play(context: Context, playlist: Playlist): ActionEntry =
        entry(ActionSemantics.Play) {
            ItemActions.play(context, playlist)
        }

    fun PlayNext(context: Context, playlist: Playlist): ActionEntry =
        entry(ActionSemantics.PlayNext) {
            ItemActions.playNext(context, playlist)
        }

    fun AddToPlayingQueue(context: Context, playlist: Playlist): ActionEntry =
        entry(ActionSemantics.AddToPlayingQueue) {
            ItemActions.addToCurrentQueue(context, playlist)
        }

    fun AddToPlaylist(context: Context, playlist: Playlist): ActionEntry =
        entry(ActionSemantics.AddToPlaylist) {
            withActivity(context) { ItemActions.addToPlaylist(it, playlist) }
        }

    fun Rename(context: Context, playlist: Playlist): ActionEntry =
        entry(ActionSemantics.Rename) {
            withActivity(context) { ItemActions.renamePlaylist(it, playlist) }
        }

    fun Pin(context: Context, playlist: Playlist): ActionEntry {
        val pined = runBlocking { PinedPlaylists.isPined(context, playlist) }
        return entry(if (pined) ActionSemantics.Unpin else ActionSemantics.Pin) {
            withContext(Dispatchers.IO) {
                PinedPlaylists.toggleState(context, playlist)
                EventHub.sendEvent(context, EventHub.EVENT_PLAYLISTS_CHANGED)
            }
        }
    }

    fun Delete(context: Context, playlist: Playlist): ActionEntry =
        entry(if (playlist.isVirtual()) ActionSemantics.Clear else ActionSemantics.Delete, dangerous = true) {
            withActivity(context) { ItemActions.deletePlaylist(it, playlist) }
        }

    fun SavePlaylist(context: Context, playlist: Playlist): ActionEntry =
        entry(ActionSemantics.SavePlaylist) {
            withActivity(context) { ItemActions.savePlaylist(it, playlist) }
        }
    //endregion

    //region FileItem
    fun Play(context: Context, file: FileItem): ActionEntry =
        entry(ActionSemantics.Play) {
            ItemActions.playNow(context, convertToSongs(context, file))
        }

    fun PlayNext(context: Context, file: FileItem): ActionEntry =
        PlayNext(context, file, ::convertToSongs)

    fun AddToPlayingQueue(context: Context, file: FileItem): ActionEntry =
        AddToPlayingQueue(context, file, ::convertToSongs)

    fun AddToPlaylist(context: Context, file: FileItem): ActionEntry =
        AddToPlaylist(context, file, ::convertToSongs)

    fun DeleteFromDevice(context: Context, file: FileItem): ActionEntry =
        DeleteFromDevice(context, file, ::convertToSongs)

    fun Details(context: Context, file: FileItem): ActionEntry =
        entry(ActionSemantics.Details) {
            withActivity(context) { activity ->
                convertToSong(context, file)?.let { song -> ItemActions.gotoDetail(activity, song) }
            }
        }

    fun Share(context: Context, file: FileItem): ActionEntry =
        entry(ActionSemantics.Share) {
            convertToSong(context, file)?.let { song -> ItemActions.share(context, song) }
        }

    fun TagEditor(context: Context, file: FileItem): ActionEntry =
        entry(ActionSemantics.TagEditor, ActionEntry.ToolbarEntry.IF_ROOM) {
            goToSongDetail(context, file.path)
        }

    fun Scan(context: Context, dir: FileItem): ActionEntry =
        entry(ActionSemantics.Scan) {
            withContext(Dispatchers.IO) {
                val files = File(dir.path).listFiles() ?: return@withContext
                val paths = Array(files.size) { files[it].path }
                MediaStoreScanner(
                    context,
                    ProgressNotificationConnection(
                        context, R.string.notification_background_name,
                        channel = NOTIFICATION_CHANNEL_ID_BACKGROUND_TASKS,
                    ),
                ).scan(paths)
            }
        }

    fun SetAsStartDirectory(context: Context, dir: FileItem): ActionEntry =
        entry(ActionSemantics.SetAsStartDirectory) {
            val path = dir.path
            Settings(context)[Keys.startDirectoryPath].data = path
            Toast.makeText(
                context,
                String.format(context.getString(R.string.msg_new_start_directory), path),
                Toast.LENGTH_SHORT
            ).show()
        }

    fun AddToBlackList(context: Context, file: FileItem): ActionEntry =
        entry(ActionSemantics.AddToBlackList) {
            val path = withContext(Dispatchers.IO) {
                val target = File(file.path)
                if (target.isDirectory) target.absolutePath
                else target.absolutePath.dropLastWhile { it != '/' }.dropLast(1)
            }
            ItemActions.addToBlacklist(context, path)
        }
    //endregion

    //region Utils
    private suspend inline fun withActivity(context: Context, block: suspend (FragmentActivity) -> Unit) {
        if (context is FragmentActivity) block(context)
    }
    //endregion
}
