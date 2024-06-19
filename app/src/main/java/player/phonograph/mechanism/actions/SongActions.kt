/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.actions

import player.phonograph.R
import player.phonograph.mechanism.PathFilter
import player.phonograph.misc.RingtoneManager
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.dialogs.DeletionDialog
import player.phonograph.ui.dialogs.SongDetailDialog
import player.phonograph.ui.modules.playlist.dialogs.AddToPlaylistDialog
import player.phonograph.ui.modules.tag.TagBrowserActivity
import player.phonograph.util.NavigationUtil
import player.phonograph.util.shareFileIntent
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.content.Intent
import android.view.View


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


internal fun actionRemoveFromQueue(index: Int): Boolean =
    MusicPlayerRemote.removeFromQueue(index)

fun Song.actionGotoDetail(activity: FragmentActivity): Boolean {
    val preference = Setting(activity)[Keys.useLegacyDetailDialog]
    if (preference.data)
        SongDetailDialog.create(this).show(activity.supportFragmentManager, "SONG_DETAILS")
    else
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
    RingtoneManager.setRingtone(context, id)

fun Song.actionAddToBlacklist(context: Context): Boolean {
    PathFilter.addToBlacklist(context, this)
    return true
}

fun Song.actionTagEditor(context: Context): Boolean {
    TagBrowserActivity.launch(context, data)
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
        DeletionDialog
            .create(ArrayList(this)).show(it.supportFragmentManager, "ADD_DELETE")
        true
    }