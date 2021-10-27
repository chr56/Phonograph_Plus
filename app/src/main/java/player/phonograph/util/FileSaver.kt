/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.app.Activity
import android.content.Context
import android.net.Uri
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.model.AbsCustomPlaylist
import player.phonograph.model.Playlist
import player.phonograph.model.Song

object FileSaver {
    fun saveFile(activity: Activity, uri: Uri, data: String): Short {
        val stream = activity.contentResolver.openOutputStream(uri) ?: return ERROR
        stream.write(data.encodeToByteArray())
        stream.close()
        activity.contentResolver.notifyChange(uri, null)

        return SUCCESS
    }

    /**
     * save Playlist as M3U
     * @param uri target file
     */
    fun savePlaylist(activity: Activity, uri: Uri, playlistId: Long): Short {
        val playlist: Playlist = MediaStoreUtil.getPlaylist(activity as Context, playlistId)
        if (playlist.id == -1L) return ERROR // id -1 -> empty/null playlist
        return savePlaylist(activity, uri, playlist)
    }

    /**
     * save Playlist as M3U
     * @param uri target file
     */
    fun savePlaylist(activity: Activity, uri: Uri, playlist: Playlist): Short {

        val songs: List<Song> =
            if (playlist is AbsCustomPlaylist) {
                playlist.getSongs(activity)
            } else {
                PlaylistSongLoader.getPlaylistSongList(activity, playlist.id)
            }

        if (songs.isEmpty()) return ERROR // no song? why save it?

        val buffer = StringBuffer()
        buffer.append(FILE_HEADER)
        for (song in songs) {
            buffer.appendLine()
            buffer.append(ENTRY_HEADER + song.duration + "," + song.artistName + " - " + song.title)
            buffer.appendLine()
            buffer.append(song.data)
        }

        return saveFile(activity, uri, buffer.toString())
    }

    private const val FILE_HEADER = "#EXTM3U"
    private const val ENTRY_HEADER = "#EXTINF:"

    private const val SUCCESS: Short = 0
    private const val ERROR: Short = (-1)
}
