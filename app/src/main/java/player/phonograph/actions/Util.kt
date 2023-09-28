/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.Albums
import player.phonograph.repo.loader.Artists
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.loaders.GenreLoader
import androidx.fragment.app.FragmentActivity
import android.app.Activity
import android.content.Context

internal fun convertToSongs(selections: Iterable<*>, context: Context): List<Song> = selections.flatMap {
    when (it) {
        is Song -> listOf(it)
        is Album -> Albums.songs(context, it.id)
        is Artist -> Artists.songs(context, it.id)
        is Genre -> GenreLoader.genreSongs(context, it.id)
        is Playlist -> it.getSongs(context)
        is FileEntity.File -> listOf(Songs.searchByFileEntity(context, it))
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
