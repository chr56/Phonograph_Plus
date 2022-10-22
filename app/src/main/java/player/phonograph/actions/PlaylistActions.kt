/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.ClearPlaylistDialog
import player.phonograph.dialogs.RenamePlaylistDialog
import player.phonograph.misc.SAFCallbackHandlerActivity
import player.phonograph.model.playlist.Playlist
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import util.phonograph.m3u.PlaylistsManager
import androidx.fragment.app.FragmentActivity
import android.content.Context

fun Playlist.actionPlay(context: Context): Boolean =
    getSongs(context).actionPlayQueue(context)

fun Playlist.actionShuffleAndPlay(context: Context) =
    MusicPlayerRemote.playQueueCautiously(getSongs(context), 0, true, ShuffleMode.SHUFFLE)

fun Playlist.actionPlayNext(context: Context): Boolean =
    MusicPlayerRemote.playNext(ArrayList(getSongs(context)))

fun Playlist.actionAddToCurrentQueue(context: Context): Boolean =
    MusicPlayerRemote.enqueue(ArrayList(getSongs(context)))

fun Playlist.actionAddToPlaylist(activity: FragmentActivity) {
    AddToPlaylistDialog.create(getSongs(activity))
        .show(activity.supportFragmentManager, "ADD_PLAYLIST")
}

fun Playlist.actionRenamePlaylist(activity: FragmentActivity) {
    RenamePlaylistDialog.create(this.id)
        .show(activity.supportFragmentManager, "RENAME_PLAYLIST")
}

fun Playlist.actionDeletePlaylist(activity: FragmentActivity) {
    ClearPlaylistDialog.create(listOf(this))
        .show(activity.supportFragmentManager, "CLEAR_PLAYLIST")
}

fun List<Playlist>.actionDeletePlaylists(activity: Context): Boolean =
    if (activity is FragmentActivity) {
        ClearPlaylistDialog.create(this)
            .show(activity.supportFragmentManager, "CLEAR_PLAYLIST")
        true
    } else {
        false
    }

fun Playlist.actionSavePlaylist(activity: FragmentActivity) {
    CoroutineScope(Dispatchers.Default).launch {
        PlaylistsManager(activity, activity as? SAFCallbackHandlerActivity)
            .duplicatePlaylistViaSaf(this@actionSavePlaylist)
    }
}

fun List<Playlist>.actionSavePlaylists(activity: Context) {
    CoroutineScope(Dispatchers.Default).launch {
        PlaylistsManager(activity, activity as? SAFCallbackHandlerActivity)
            .duplicatePlaylistsViaSaf(this@actionSavePlaylists)
    }
}