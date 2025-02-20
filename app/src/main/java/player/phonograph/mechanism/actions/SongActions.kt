/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.actions

import player.phonograph.R
import player.phonograph.mechanism.PathFilter
import player.phonograph.model.Song
import player.phonograph.repo.loader.Playlists
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.dialogs.DeletionDialog
import player.phonograph.ui.modules.playlist.dialogs.AddToPlaylistDialogActivity
import player.phonograph.ui.modules.tag.TagBrowserActivity
import player.phonograph.util.NavigationUtil
import player.phonograph.util.fragmentActivity
import player.phonograph.util.permissions.checkModificationSystemSettingsPermission
import player.phonograph.util.setRingtone
import player.phonograph.util.shareFileIntent
import player.phonograph.util.theme.tintButtons
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


fun Song.actionPlay(): Boolean = actionPlayNow()

/**
 * Play queue with target [ShuffleMode]
 */
fun List<Song>.actionPlay(shuffleMode: ShuffleMode?, position: Int) =
    MusicPlayerRemote.playQueue(this, position, true, shuffleMode)

fun Song.actionPlayNow(): Boolean =
    MusicPlayerRemote.playNow(this)

fun List<Song>.actionPlayNow(): Boolean =
    MusicPlayerRemote.playNow(this)

fun Song.actionPlayNext(): Boolean =
    MusicPlayerRemote.playNext(this)

fun List<Song>.actionPlayNext(): Boolean =
    MusicPlayerRemote.playNext(this)


fun Song.actionEnqueue(): Boolean =
    MusicPlayerRemote.enqueue(this)

fun List<Song>.actionEnqueue(): Boolean =
    MusicPlayerRemote.enqueue(this)


fun Song.actionGotoDetail(activity: FragmentActivity): Boolean {
    TagBrowserActivity.launch(activity, data)
    return true
}

fun Song.actionGotoAlbum(context: Context, transitionView: View?): Boolean =
    if (transitionView != null) {
        NavigationUtil.goToAlbum(
            context,
            albumId,
            Pair(transitionView, context.resources.getString(R.string.transition_album_art))
        )
        true
    } else {
        NavigationUtil.goToAlbum(context, albumId)
        true
    }


fun Song.actionGotoArtist(context: Context, transitionView: View?): Boolean {
    val sharedElements: Array<Pair<View, String>>? =
        transitionView?.let { arrayOf(Pair(it, context.resources.getString(R.string.transition_artist_image))) }
    if (artistName != null) {
        NavigationUtil.goToArtist(context, artistName, sharedElements)
    } else {
        NavigationUtil.goToArtist(context, artistId, sharedElements)
    }
    return true
}

fun Song.actionShare(context: Context): Boolean {
    context.startActivity(
        Intent.createChooser(
            shareFileIntent(context, this), null
        )
    )
    return true
}

fun Song.actionSetAsRingtone(context: Context): Boolean =
    if (checkModificationSystemSettingsPermission(context)) {
        showRingtoneDialog(context)
        true
    } else {
        setRingtone(context, id)
        true
    }


fun Song.actionAddToBlacklist(context: Context): Boolean {
    PathFilter.addToBlacklist(context, this)
    return true
}

fun Song.actionTagEditor(context: Context): Boolean {
    TagBrowserActivity.launch(context, data)
    return true
}

fun List<Song>.actionAddToPlaylist(context: Context) =
    fragmentActivity(context) { activity ->
        activity.lifecycleScope.launch {
            val songs = this@actionAddToPlaylist
            val playlists = withContext(Dispatchers.IO) { Playlists.all(activity) }
            activity.startActivity(
                AddToPlaylistDialogActivity.Parameter.buildLaunchingIntent(activity, songs, playlists)
            )
        }
        true
    }

fun List<Song>.actionDelete(context: Context) =
    fragmentActivity(context) {
        DeletionDialog
            .create(ArrayList(this)).show(it.supportFragmentManager, "ADD_DELETE")
        true
    }

private fun showRingtoneDialog(context: Context): AlertDialog =
    AlertDialog.Builder(context)
        .setTitle(R.string.dialog_ringtone_title)
        .setMessage(R.string.dialog_ringtone_title)
        .setNegativeButton(android.R.string.cancel) { d, _ -> d.dismiss() }
        .setPositiveButton(android.R.string.ok) { _, _ ->
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        }
        .create().tintButtons()