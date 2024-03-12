/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.model.Song
import android.content.Context

object PlaylistType {
    const val FILE = 0
    const val ABS_SMART = 1
    const val FAVORITE = 2
    const val LAST_ADDED = 4
    const val HISTORY = 8
    const val MY_TOP_TRACK = 16
    const val RANDOM = 32
    const val DATABASE = 128
}

interface EditablePlaylist : ResettablePlaylist {
    fun removeSong(context: Context, song: Song)
    fun removeSongs(context: Context, songs: List<Song>) {
        for (song in songs) {
            removeSong(context, song)
        }
    }

    fun appendSong(context: Context, song: Song)
    fun appendSongs(context: Context, songs: List<Song>) {
        for (song in songs) {
            appendSong(context, song)
        }
    }

    fun moveSong(context: Context, song: Song, from: Int, to: Int)
//    fun insert(context: Context, song: Song, pos: Int)
}

interface ResettablePlaylist {
    fun clear(context: Context)
}

interface GeneratedPlaylist {
    fun refresh(context: Context) {}
}