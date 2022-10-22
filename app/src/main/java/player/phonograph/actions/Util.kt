/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import android.content.Context
import androidx.fragment.app.FragmentActivity
import player.phonograph.mediastore.GenreLoader
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.linkedSong
import player.phonograph.model.playlist.Playlist
import android.app.Activity

internal fun convertToSongs(selections: List<Any>, context: Context): List<Song> = selections.flatMap {
    when (it) {
        is Song -> listOf(it)
        is Album -> it.songs
        is Artist -> it.songs
        is Genre -> GenreLoader.getSongs(context, it.id)
        is Playlist -> it.getSongs(context)
        is FileEntity.File -> listOf(it.linkedSong(context))
        // is FileEntity.Folder -> TODO()
        else -> emptyList()
    }
}


inline fun activity(context: Context, block: (Activity) -> Boolean): Boolean =
    if (context is Activity) {
        block(context)
    } else {
        false
    }

inline fun fragmentActivity(context: Context, block: (FragmentActivity) -> Boolean): Boolean =
    if (context is FragmentActivity) {
        block(context)
    } else {
        false
    }
