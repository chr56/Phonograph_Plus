/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.actions

import player.phonograph.model.Song
import player.phonograph.model.file.FileItem
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.playlist.Playlist
import android.content.Context

@Suppress("FunctionName")
object ActionMenus {

    fun Song(context: Context, song: Song, showPlay: Boolean, index: Int = Int.MIN_VALUE): List<ActionEntry> = buildList {
        if (showPlay) add(ActionEntries.Play(context, song))
        add(ActionEntries.PlayNext(context, song))
        if (index >= 0) {
            add(ActionEntries.RemoveFromPlayingQueue(context, song, index))
        } else {
            add(ActionEntries.AddToPlayingQueue(context, song))
        }
        add(ActionEntries.AddToPlaylist(context, song))
        add(ActionEntries.GoToAlbum(context, song))
        add(ActionEntries.GoToArtist(context, song))
        add(ActionEntries.Details(context, song))
        add(
            ActionEntries.More(
                context, listOf(
                    ActionEntries.Share(context, song),
                    ActionEntries.TagEditor(context, song),
                    ActionEntries.SetAsRingtone(context, song),
                    ActionEntries.AddToBlackList(context, song),
                    ActionEntries.DeleteFromDevice(context, song),
                )
            )
        )
    }

    // Albums/Artists/Genres/SongCollections
    fun <I> Composite(context: Context, item: I, readSongs: suspend (Context, I) -> List<Song>): List<ActionEntry> = listOf(
        ActionEntries.Play(context, item, readSongs),
        ActionEntries.PlayNext(context, item, readSongs),
        ActionEntries.AddToPlayingQueue(context, item, readSongs),
        ActionEntries.AddToPlaylist(context, item, readSongs),
        ActionEntries.More(context, listOf(ActionEntries.DeleteFromDevice(context, item, readSongs))),
    )

    fun Playlist(context: Context, playlist: Playlist): List<ActionEntry> = buildList {
        add(ActionEntries.Play(context, playlist))
        add(ActionEntries.PlayNext(context, playlist))
        add(ActionEntries.AddToPlayingQueue(context, playlist))
        add(ActionEntries.AddToPlaylist(context, playlist))
        if (!playlist.isVirtual()) {
            add(ActionEntries.Rename(context, playlist))
            add(ActionEntries.Pin(context, playlist))
        }
        add(ActionEntries.Delete(context, playlist))
        add(ActionEntries.SavePlaylist(context, playlist))
    }

    fun FileItem(context: Context, file: FileItem): List<ActionEntry> = buildList {
        add(ActionEntries.Play(context, file))
        add(ActionEntries.PlayNext(context, file))
        add(ActionEntries.AddToPlayingQueue(context, file))
        add(ActionEntries.AddToPlaylist(context, file))
        if (file.isFile) {
            add(ActionEntries.Details(context, file))
            add(ActionEntries.Share(context, file))
            add(ActionEntries.TagEditor(context, file))
        } else {
            add(ActionEntries.Scan(context, file))
            add(ActionEntries.SetAsStartDirectory(context, file))
            add(ActionEntries.AddToBlackList(context, file))
        }
        add(ActionEntries.DeleteFromDevice(context, file))
    }
}
