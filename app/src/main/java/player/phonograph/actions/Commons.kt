/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import android.content.Context
import androidx.fragment.app.FragmentActivity
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.mediastore.GenreLoader
import player.phonograph.mediastore.MediaStoreUtil.linkedSong
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.ui.dialogs.DeleteSongsDialog

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