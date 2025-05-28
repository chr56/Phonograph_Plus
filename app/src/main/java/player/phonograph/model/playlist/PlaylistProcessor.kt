/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.model.Song
import android.content.Context

sealed interface PlaylistProcessor

interface PlaylistReader : PlaylistProcessor {
    suspend fun allSongs(context: Context): List<Song>
    suspend fun containsSong(context: Context, songId: Long): Boolean
    suspend fun refresh(context: Context) {}
}

interface PlaylistWriter : PlaylistProcessor {
    suspend fun removeSong(context: Context, song: Song, index: Long): Boolean
    suspend fun removeSongs(context: Context, songs: List<Song>) {
        for (song in songs) {
            removeSong(context, song, -1)
        }
    }

    //    fun insert(context: Context, song: Song, pos: Int)
    suspend fun appendSong(context: Context, song: Song)
    suspend fun appendSongs(context: Context, songs: List<Song>) {
        for (song in songs) {
            appendSong(context, song)
        }
    }

    suspend fun moveSong(context: Context, from: Int, to: Int): Boolean
    suspend fun rename(context: Context, newName: String): Boolean = false
}