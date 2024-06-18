/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.file.FileEntity
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.Songs
import androidx.fragment.app.FragmentActivity
import android.app.Activity
import android.content.Context

internal suspend fun convertToSongs(selections: Iterable<*>, context: Context): List<Song> = selections.flatMap {
    when (it) {
        is Song -> listOf(it)
        is Album -> Songs.album(context, it.id)
        is Artist -> Songs.artist(context, it.id)
        is Genre -> Songs.genres(context, it.id)
        is Playlist -> PlaylistProcessors.of(it).allSongs(context)
        is SongCollection -> it.songs
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
