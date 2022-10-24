/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import player.phonograph.R
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.SongDetailDialog
import player.phonograph.dialogs.SongShareDialog
import player.phonograph.interfaces.PaletteColorHolder
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.DetailActivity
import player.phonograph.ui.dialogs.DeleteSongsDialog
import player.phonograph.util.NavigationUtil
import player.phonograph.util.PathFilterUtil
import player.phonograph.util.RingtoneManager
import util.phonograph.tageditor.AbsTagEditorActivity
import util.phonograph.tageditor.SongTagEditorActivity
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.content.Intent
import android.view.View


fun Song.actionPlay(): Boolean = actionPlayNow()

/**
 * Play queue.
 *
 * This would ask for ShuffleMode if [Setting.rememberShuffle] enabled.
 * @param shuffleMode target shuffle mode
 */
fun List<Song>.actionPlay(context: Context, shuffleMode: ShuffleMode) =
    if (Setting.instance.rememberShuffle) {
        // ask for
        MaterialAlertDialogBuilder(context)
            .setMessage(R.string.pref_title_remember_shuffle)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                MusicPlayerRemote.playQueue(this, 0, true, null)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                MusicPlayerRemote.playQueue(this, 0, true, ShuffleMode.NONE)
            }.create().show()
        true
    } else {
        MusicPlayerRemote.playQueue(this, 0, true, shuffleMode)
    }

fun Song.actionPlayNow(): Boolean =
    MusicPlayerRemote.playNow(this)

fun Song.actionPlayNext(): Boolean =
    MusicPlayerRemote.playNext(this)

fun List<Song>.actionPlayNext(): Boolean =
    MusicPlayerRemote.playNext(this)


fun Song.actionEnqueue(): Boolean =
    MusicPlayerRemote.enqueue(this)

fun List<Song>.actionEnqueue(): Boolean =
    MusicPlayerRemote.enqueue(this)


internal fun actionRemoveFromQueue(index: Int): Boolean =
    MusicPlayerRemote.removeFromQueue(index)

fun Song.actionGotoDetail(activity: FragmentActivity): Boolean {
    if (Setting.instance().useLegacyDetailDialog)
        SongDetailDialog.create(this).show(activity.supportFragmentManager, "SONG_DETAILS")
    else
        activity.startActivity(Intent(activity, DetailActivity::class.java).apply {
            putExtra("song", this@actionGotoDetail)
        })
    return true
}

fun Song.actionGotoAlbum(context: Context, transitionView: View?): Boolean =
    activity(context) {
        if (transitionView != null) {
            NavigationUtil.goToAlbum(
                it,
                albumId,
                Pair(transitionView, context.resources.getString(R.string.transition_album_art))
            )
        } else {
            NavigationUtil.goToAlbum(it, albumId)
        }
        true
    }


fun Song.actionGotoArtist(context: Context, transitionView: View?): Boolean =
    activity(context) {
        if (transitionView != null) {
            NavigationUtil.goToArtist(
                it,
                artistId,
                Pair(transitionView,
                     context.resources.getString(R.string.transition_artist_image))
            )
        } else {
            NavigationUtil.goToArtist(it, artistId)
        }
        true
    }

fun Song.actionShare(context: Context): Boolean {
    context.startActivity(
        Intent.createChooser(
            SongShareDialog.createShareSongFileIntent(this, context), null
        )
    )
    return true
}

fun Song.actionSetAsRingtone(context: Context): Boolean {
    if (RingtoneManager.requiresDialog(context)) {
        RingtoneManager.showDialog(context)
    } else {
        RingtoneManager.setRingtone(context, id)
    }
    return true
}

fun Song.actionAddToBlacklist(context: Context): Boolean {
    PathFilterUtil.addToBlacklist(context, this)
    return true
}

fun Song.actionTagEditor(context: Context): Boolean {
    context.startActivity(Intent(context, SongTagEditorActivity::class.java).apply {
        putExtra(AbsTagEditorActivity.EXTRA_ID, id)
        (context as? PaletteColorHolder)?.let {
            putExtra(AbsTagEditorActivity.EXTRA_PALETTE, it.paletteColor)
        }
    })
    return true
}

fun List<Song>.actionAddToPlaylist(context: Context) =
    fragmentActivity(context) {
        AddToPlaylistDialog
            .create(this).show(it.supportFragmentManager, "ADD_PLAYLIST")
        true
    }

fun List<Song>.actionDelete(context: Context) =
    fragmentActivity(context) {
        DeleteSongsDialog
            .create(ArrayList(this)).show(it.supportFragmentManager, "ADD_DELETE")
        true
    }