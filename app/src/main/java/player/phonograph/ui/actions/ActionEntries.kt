/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.actions

import player.phonograph.R
import player.phonograph.mechanism.event.EventHub
import player.phonograph.mechanism.scanner.MediaStoreScanner
import player.phonograph.model.Song
import player.phonograph.model.file.FileItem
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.menu.ActionSemantic
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.PinedPlaylists
import player.phonograph.repo.loader.Songs
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import player.phonograph.ui.modules.tag.TagBrowserActivity
import player.phonograph.ui.resource.ActionSemantics
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
        entry(ActionSemantics.Play) { song.actionPlay() }

    fun PlayNext(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.PlayNext) { song.actionPlayNext() }

    fun AddToPlayingQueue(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.AddToPlayingQueue) { song.actionEnqueue() }

    fun RemoveFromPlayingQueue(context: Context, song: Song, index: Int): ActionEntry =
        entry(ActionSemantics.RemoveFromPlayingQueue) {
            MusicPlayerRemote.queueManager.removeSongAt(index)
        }

    fun AddToPlaylist(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.AddToPlaylist) {
            listOf(song).actionAddToPlaylist(context)
        }

    fun GoToAlbum(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.GoToAlbum) {
            song.actionGotoAlbum(context)
        }

    fun GoToArtist(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.GoToArtist) {
            song.actionGotoArtist(context)
        }

    fun Details(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.Details) {
            withActivity(context) { song.actionGotoDetail(it) }
        }

    fun Share(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.Share) { song.actionShare(context) }

    fun TagEditor(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.TagEditor) { song.actionTagEditor(context) }

    fun SetAsRingtone(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.SetAsRingtone) { song.actionSetAsRingtone(context) }

    fun AddToBlackList(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.AddToBlackList) {
            song.actionAddToBlacklist(context)
        }

    fun DeleteFromDevice(context: Context, song: Song): ActionEntry =
        entry(ActionSemantics.DeleteFromDevice, dangerous = true) {
            listOf(song).actionDelete(context)
        }
    //endregion

    //region Composite
    fun <I> Play(context: Context, item: I, readSongs: suspend (Context, I) -> List<Song>): ActionEntry =
        entry(ActionSemantics.Play) {
            readSongs(context, item).actionPlay(ShuffleMode.NONE, 0)
        }

    fun <I> PlayNext(context: Context, item: I, readSongs: suspend (Context, I) -> List<Song>): ActionEntry =
        entry(ActionSemantics.PlayNext) {
            readSongs(context, item).actionPlayNext()
        }

    fun <I> AddToPlayingQueue(context: Context, item: I, readSongs: suspend (Context, I) -> List<Song>): ActionEntry =
        entry(ActionSemantics.AddToPlayingQueue) {
            readSongs(context, item).actionEnqueue()
        }

    fun <I> AddToPlaylist(context: Context, item: I, readSongs: suspend (Context, I) -> List<Song>): ActionEntry =
        entry(ActionSemantics.AddToPlaylist) {
            withActivity(context) { readSongs(context, item).actionAddToPlaylist(it) }
        }

    fun <I> DeleteFromDevice(context: Context, item: I, readSongs: suspend (Context, I) -> List<Song>): ActionEntry =
        entry(ActionSemantics.DeleteFromDevice, dangerous = true) {
            withActivity(context) { readSongs(context, item).actionDelete(it) }
        }
    //endregion

    //region Playlist
    fun Play(context: Context, playlist: Playlist): ActionEntry =
        entry(ActionSemantics.Play) {
            playlist.actionPlay(context)
        }

    fun PlayNext(context: Context, playlist: Playlist): ActionEntry =
        entry(ActionSemantics.PlayNext) {
            playlist.actionPlayNext(context)
        }

    fun AddToPlayingQueue(context: Context, playlist: Playlist): ActionEntry =
        entry(ActionSemantics.AddToPlayingQueue) {
            playlist.actionAddToCurrentQueue(context)
        }

    fun AddToPlaylist(context: Context, playlist: Playlist): ActionEntry =
        entry(ActionSemantics.AddToPlaylist) {
            withActivity(context) { playlist.actionAddToPlaylist(it) }
        }

    fun Rename(context: Context, playlist: Playlist): ActionEntry =
        entry(ActionSemantics.Rename) {
            withActivity(context) { playlist.actionRenamePlaylist(it) }
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
            withActivity(context) { playlist.actionDeletePlaylist(it) }
        }

    fun SavePlaylist(context: Context, playlist: Playlist): ActionEntry =
        entry(ActionSemantics.SavePlaylist) {
            withActivity(context) { playlist.actionSavePlaylist(it) }
        }
    //endregion

    //region FileItem
    fun Play(context: Context, file: FileItem): ActionEntry =
        entry(ActionSemantics.Play) {
            readSongs(context, file).actionPlayNow()
        }

    fun PlayNext(context: Context, file: FileItem): ActionEntry =
        PlayNext(context, file, ::readSongs)

    fun AddToPlayingQueue(context: Context, file: FileItem): ActionEntry =
        AddToPlayingQueue(context, file, ::readSongs)

    fun AddToPlaylist(context: Context, file: FileItem): ActionEntry =
        AddToPlaylist(context, file, ::readSongs)

    fun DeleteFromDevice(context: Context, file: FileItem): ActionEntry =
        DeleteFromDevice(context, file, ::readSongs)

    fun Details(context: Context, file: FileItem): ActionEntry =
        entry(ActionSemantics.Details) {
            withActivity(context) { readSong(context, file)?.actionGotoDetail(it) }
        }

    fun Share(context: Context, file: FileItem): ActionEntry =
        entry(ActionSemantics.Share) {
            readSong(context, file)?.actionShare(context)
        }

    fun TagEditor(context: Context, file: FileItem): ActionEntry =
        entry(ActionSemantics.TagEditor, ActionEntry.ToolbarEntry.IF_ROOM) {
            TagBrowserActivity.launch(context, file.path)
        }

    fun Scan(context: Context, dir: FileItem): ActionEntry =
        entry(ActionSemantics.Scan) {
            withContext(Dispatchers.IO) {
                val files = File(dir.path).listFiles() ?: return@withContext
                val paths = Array(files.size) { files[it].path }
                MediaStoreScanner(context).scan(paths)
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
            addToBlacklist(context, path)
        }
    //endregion

    //region Utils
    private suspend inline fun withActivity(context: Context, block: suspend (FragmentActivity) -> Unit) {
        if (context is FragmentActivity) block(context)
    }

    private suspend fun readSong(context: Context, file: FileItem): Song? =
        withContext(Dispatchers.IO) {
            if (file.content is FileItem.SongContent) {
                file.content.song
            } else if (file.mediaPath.mediastoreId > 0) {
                Songs.id(context, file.mediaPath.mediastoreId)
            } else {
                Songs.path(context, file.path, withoutPathFilter = true)
            }
        }

    private suspend fun readSongs(context: Context, file: FileItem): List<Song> =
        withContext(Dispatchers.IO) { file.songs(context) }
    //endregion
}
