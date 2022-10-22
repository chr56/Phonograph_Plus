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
import util.phonograph.tageditor.AbsTagEditorActivity
import util.phonograph.tageditor.SongTagEditorActivity
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.content.Intent

fun Song.actionsGotoDetail(activity: FragmentActivity): Boolean {
    if (Setting.instance().useLegacyDetailDialog)
        SongDetailDialog.create(this).show(activity.supportFragmentManager, "SONG_DETAILS")
    else
        activity.startActivity(Intent(activity, DetailActivity::class.java).apply {
            putExtra("song", this)
        })
    return true
}

fun Song.actionShare(context: Context): Boolean {
    context.startActivity(
        Intent.createChooser(
            SongShareDialog.createShareSongFileIntent(this, context), null
        )
    )
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

fun List<Song>.actionPlayQueue(context: Context) =
    if (Setting.instance.rememberShuffle) {
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
        MusicPlayerRemote.playQueue(this, 0, true, ShuffleMode.NONE)
    }

fun List<Song>.actionAddToPlaylist(context: Context) =
    if ((context is FragmentActivity)) {
        AddToPlaylistDialog.create(this).show(context.supportFragmentManager, "ADD_PLAYLIST")
        true
    } else {
        false
    }

fun List<Song>.actionDelete(context: Context) =
    if ((context is FragmentActivity)) {
        DeleteSongsDialog.create(ArrayList(this))
            .show(context.supportFragmentManager, "ADD_DELETE")
        true
    } else {
        false
    }