/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.service.queue

import player.phonograph.R
import player.phonograph.model.PlayRequest.SongsRequest
import player.phonograph.model.Song
import player.phonograph.repo.loader.Songs
import android.content.Context


object QueueValidator {

    suspend fun removeMissingSongs(context: Context, queue: SongsRequest): SongsRequest {
        val all = queue.songs
        val current = queue.position
        val left = all.subList(0, current)
        val right = all.subList(current, all.size)
        val newLeft = left.mapNotNull { song -> findSong(context, song) }
        val newRight = right.mapNotNull { song -> findSong(context, song) }
        return SongsRequest(newLeft + newRight, newLeft.size)
    }

    suspend fun markInvalidSongs(context: Context, songs: List<Song>): List<Song> {
        return songs.map { song ->
            findSong(context, song) ?: markDeleted(context, song)
        }
    }

    private suspend fun findSong(context: Context, song: Song): Song? =
        Songs.id(context, song.id) ?: Songs.path(context, song.data)

    private fun markDeleted(context: Context, song: Song): Song {
        val prefix = "[${context.getString(R.string.state_deleted)}]"
        val title = song.title
        val new = title.takeIf { title.startsWith(prefix) } ?: (prefix + title)
        return song.copy(title = new)
    }
}