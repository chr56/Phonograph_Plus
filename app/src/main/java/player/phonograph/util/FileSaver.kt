/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import kotlinx.coroutines.async
import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.model.AbsCustomPlaylist
import player.phonograph.model.Playlist
import player.phonograph.model.Song

object FileSaver {
    fun saveFile(activity: Activity, uri: Uri, data: String): Int {
        val stream = activity.contentResolver.openOutputStream(uri) ?: return ERROR
        stream.write(data.encodeToByteArray())
        stream.close()
        activity.contentResolver.notifyChange(uri, null)

        LocalBroadcastManager.getInstance(App.instance).sendBroadcast(Intent(BROADCAST_PLAYLISTS_CHANGED))

        return SUCCESS
    }

    /**
     * save Playlist as M3U
     * @param uri target file
     */
    fun savePlaylist(activity: Activity, uri: Uri, playlistId: Long): Deferred<Int> {
        val playlist: Playlist = MediaStoreUtil.getPlaylist(activity as Context, playlistId)
        return savePlaylist(activity, uri, playlist)
    }

    /**
     * save Playlist as M3U
     * @param uri target file
     */
    fun savePlaylist(activity: Activity, uri: Uri, playlist: Playlist): Deferred<Int> {
        val result = GlobalScope.async(Dispatchers.IO) {
            if (playlist.id == -1L) return@async ERROR // id -1 -> empty/null playlist

            if (!isActive) return@async CANCELED
            val songs: List<Song> =
                if (playlist is AbsCustomPlaylist) {
                    playlist.getSongs(activity)
                } else {
                    PlaylistSongLoader.getPlaylistSongList(activity, playlist.id)
                }

            if (songs.isEmpty()) return@async EMPTY // no song? why save it?

            val buffer = StringBuffer()
            buffer.append(FILE_HEADER)
            for (song in songs) {
                if (!isActive) return@async CANCELED
                buffer.appendLine()
                buffer.append(ENTRY_HEADER + song.duration + "," + song.artistName + " - " + song.title)
                buffer.appendLine()
                buffer.append(song.data)
            }

            return@async saveFile(activity, uri, buffer.toString())
        }

        return result
    }

    private const val FILE_HEADER = "#EXTM3U"
    private const val ENTRY_HEADER = "#EXTINF:"

    private const val SUCCESS: Int = 0
    private const val ERROR: Int = 1
    private const val CANCELED: Int = 2
    private const val EMPTY: Int = 3
}
