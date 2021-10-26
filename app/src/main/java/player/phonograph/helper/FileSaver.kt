/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph.helper

import android.app.Activity
import android.content.Context
import android.net.Uri
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.model.AbsCustomPlaylist
import player.phonograph.model.Playlist
import player.phonograph.model.Song
import player.phonograph.util.MediaStoreUtil

object FileSaver {
    fun saveFile(activity: Activity, uri: Uri, data: String): Short {
        val stream = activity.contentResolver.openOutputStream(uri) ?: return -1

        stream.write(data.encodeToByteArray())
        return 0

    }

    /**
     * save Playlist as M3U
     * @param uri target file
     */
    fun savePlaylist(activity: Activity, uri: Uri, playlistId: Long): Short {

        val stream = activity.contentResolver.openOutputStream(uri) ?: return -1

        val playlist: Playlist = MediaStoreUtil.getPlaylist(activity as Context, playlistId)
        if (playlist.id == -1L) return -1

        val songs: List<Song> =
            if (playlist is AbsCustomPlaylist) {
                playlist.getSongs(activity)
            } else {
                PlaylistSongLoader.getPlaylistSongList(activity, playlist.id)
            }

        if (songs.isEmpty()) return -1

//        val bw = BufferedWriter
        val b = StringBuffer()
        b.append(FILE_HEADER)
        for (song in songs) {
            b.appendLine()
            b.append(ENTRY_HEADER + song.duration + "," + song.artistName + " - " + song.title)
            b.appendLine()
            b.append(song.data)
        }

        stream.write(b.toString().encodeToByteArray())
        stream.close()
        return 0
    }
    /**
     * save Playlist as M3U
     * @param uri target file
     */
    fun savePlaylist(activity: Activity, uri: Uri, playlist: Playlist): Short {

        val stream = activity.contentResolver.openOutputStream(uri) ?: return -1


        val songs: List<Song> =
            if (playlist is AbsCustomPlaylist) {
                playlist.getSongs(activity)
            } else {
                PlaylistSongLoader.getPlaylistSongList(activity, playlist.id)
            }

        if (songs.isEmpty()) return -1

//        val bw = BufferedWriter
        val b = StringBuffer()
        b.append(FILE_HEADER)
        for (song in songs) {
            b.appendLine()
            b.append(ENTRY_HEADER + song.duration + "," + song.artistName + " - " + song.title)
            b.appendLine()
            b.append(song.data)
        }

        stream.write(b.toString().encodeToByteArray())
        stream.close()
        return 0
    }

    private const val FILE_HEADER = "#EXTM3U"
    private const val ENTRY_HEADER = "#EXTINF:"
}
