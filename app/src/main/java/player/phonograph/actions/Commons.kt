/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import player.phonograph.R
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.SongDetailDialog
import player.phonograph.dialogs.SongShareDialog
import player.phonograph.interfaces.PaletteColorHolder
import player.phonograph.mediastore.GenreLoader
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.linkedSong
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Setting
import player.phonograph.ui.compose.DetailActivity
import player.phonograph.ui.dialogs.DeleteSongsDialog
import util.phonograph.tageditor.AbsTagEditorActivity
import util.phonograph.tageditor.SongTagEditorActivity

internal fun convertToSongs(selections: List<Any>, context: Context): List<Song> = selections.flatMap {
    when (it) {
        is Song -> listOf(it)
        is Album -> it.songs
        is Artist -> it.songs
        is Genre -> GenreLoader.getSongs(context, it.id)
        is FileEntity.File -> listOf(it.linkedSong(context))
        // is FileEntity.Folder -> TODO()
        else -> emptyList()
    }
}


internal fun Context.actionAddToPlaylist(songs: List<Song>) =
    if ((this is FragmentActivity)) {
        AddToPlaylistDialog.create(songs)
            .show(supportFragmentManager, "ADD_PLAYLIST")
        true
    } else {
        false
    }

internal fun Context.actionDelete(songs: List<Song>) =
    if ((this is FragmentActivity)) {
        DeleteSongsDialog.create(ArrayList(songs))
            .show(supportFragmentManager, "ADD_DELETE")
        true
    } else {
        false
    }

internal inline fun fragmentActivity(context: Context, block: (FragmentActivity) -> Boolean): Boolean =
    if (context is FragmentActivity) {
        block(context)
    } else {
        false
    }

fun gotoDetail(activity: FragmentActivity, song: Song): Boolean {
    if (Setting.instance().useLegacyDetailDialog)
        SongDetailDialog.create(song).show(activity.supportFragmentManager, "SONG_DETAILS")
    else
        activity.startActivity(Intent(activity, DetailActivity::class.java).apply {
            putExtra("song", song)
        })
    return true
}

fun share(context: Context, song: Song): Boolean {
    context.startActivity(Intent.createChooser(SongShareDialog.createShareSongFileIntent(song, context), null))
    return true
}


fun tagEditor(context: Context, song: Song): Boolean {
    context.startActivity(Intent(context, SongTagEditorActivity::class.java).apply {
        putExtra(AbsTagEditorActivity.EXTRA_ID, song.id)
        (context as? PaletteColorHolder)?.let {
            putExtra(AbsTagEditorActivity.EXTRA_PALETTE, it.paletteColor)
        }
    })
    return true
}

fun playQueue(context: Context, songs: List<Song>) =
    if (Setting.instance.rememberShuffle) {
        MaterialAlertDialogBuilder(context)
            .setMessage(R.string.pref_title_remember_shuffle)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                MusicPlayerRemote
                    .playQueue(songs, 0, true, null)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                MusicPlayerRemote
                    .playQueue(songs, 0, true, ShuffleMode.NONE)
            }.create().show()
        true
    } else {
        MusicPlayerRemote
            .playQueue(songs, 0, true, ShuffleMode.NONE)
    }
