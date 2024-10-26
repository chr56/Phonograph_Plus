/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.actions

import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.Playlists
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.modules.playlist.dialogs.AddToPlaylistDialogActivity
import player.phonograph.ui.modules.playlist.dialogs.ClearPlaylistDialogActivity
import player.phonograph.ui.modules.playlist.dialogs.CreatePlaylistDialogActivity
import player.phonograph.ui.modules.playlist.dialogs.RenamePlaylistDialogActivity
import player.phonograph.util.fragmentActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import android.content.Context
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun Playlist.actionPlay(context: Context): Boolean = runBlocking {
    songs(context).let { songs ->
        if (songs.isNotEmpty())
            songs.actionPlay(ShuffleMode.NONE, 0)
        else
            false
    }
}

fun Playlist.actionShuffleAndPlay(context: Context) = runBlocking {
    songs(context).let { songs ->
        if (songs.isNotEmpty())
            songs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(songs.size))
        else
            false
    }
}

fun Playlist.actionPlayNext(context: Context): Boolean = runBlocking {
    MusicPlayerRemote.playNext(ArrayList(songs(context)))
}

fun Playlist.actionAddToCurrentQueue(context: Context): Boolean = runBlocking {
    MusicPlayerRemote.enqueue(ArrayList(songs(context)))
}

fun Playlist.actionAddToPlaylist(activity: FragmentActivity) = activity.lifecycleScope.launch {
    val songs = withContext(Dispatchers.IO) { songs(activity) }
    val playlists = withContext(Dispatchers.IO) { Playlists.all(activity) }
    activity.startActivity(
        AddToPlaylistDialogActivity.Parameter.buildLaunchingIntent(activity, songs, playlists)
    )
}

fun Playlist.actionRenamePlaylist(activity: FragmentActivity) {
    activity.startActivity(
        RenamePlaylistDialogActivity.Parameter.buildLaunchingIntent(activity, this)
    )
}

fun Playlist.actionDeletePlaylist(activity: FragmentActivity) {
    activity.startActivity(
        ClearPlaylistDialogActivity.Parameter.buildLaunchingIntent(activity, listOf(this))
    )
}

fun List<Playlist>.actionDeletePlaylists(context: Context): Boolean = fragmentActivity(context) { activity ->
    activity.startActivity(
        ClearPlaylistDialogActivity.Parameter.buildLaunchingIntent(activity, this)
    )
    true
}

fun Playlist.actionSavePlaylist(activity: FragmentActivity) {
    activity.lifecycleScope.launch(Dispatchers.IO) {
        val songs = PlaylistProcessors.reader(this@actionSavePlaylist).allSongs(activity)
        withContext(Dispatchers.Main) {
            activity.startActivity(
                CreatePlaylistDialogActivity.Parameter.buildLaunchingIntentForDuplicate(
                    activity,
                    songs, name
                )
            )
        }
    }
}

fun List<Playlist>.actionSavePlaylists(context: Context) = fragmentActivity(context) { activity ->
    activity.lifecycleScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            activity.startActivity(
                CreatePlaylistDialogActivity.Parameter.buildLaunchingIntentForDuplicate(
                    activity,
                    this@actionSavePlaylists
                )
            )
        }
    }
    true
}

private suspend fun Playlist.songs(context: Context): List<Song> =
    PlaylistProcessors.reader(this).allSongs(context)