/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.service.queue

import player.phonograph.model.Song
import player.phonograph.repo.loader.Songs
import android.content.Context
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

suspend fun validSongs(context: Context, songs: List<Song>): List<Song> = coroutineScope {

    val flow = flow {
        for (it in songs) {
            emit(it)
        }
    }

    flow
        .map { song ->
            findSong(context, song) ?: song.copy(title = "[Deleted]" + song.title)
        }
        .toList()

}

private fun findSong(context: Context, song: Song): Song? =
    Songs.id(context, song.id).takeIf { Song.EMPTY_SONG != it }
        ?: Songs.path(context, song.data).takeIf { Song.EMPTY_SONG != it }