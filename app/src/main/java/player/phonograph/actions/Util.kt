/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.linkedSong
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.mediastore.loaders.GenreLoader
import androidx.fragment.app.FragmentActivity
import android.app.Activity
import android.content.Context

internal fun convertToSongs(selections: Iterable<Any>, context: Context): List<Song> = selections.flatMap {
    when (it) {
        is Song -> listOf(it)
        is Album -> it.songs
        is Artist -> it.songs
        is Genre -> GenreLoader.genreSongs(context, it.id)
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
