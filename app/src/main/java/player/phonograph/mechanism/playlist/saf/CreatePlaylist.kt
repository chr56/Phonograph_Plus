/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist.saf

import player.phonograph.mechanism.broadcast.sentPlaylistChangedLocalBoardCast
import player.phonograph.mechanism.playlist.m3u.M3UWriter
import player.phonograph.model.Song
import player.phonograph.util.openOutputStreamSafe
import player.phonograph.util.reportError
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.delay

/**
 * @param uri content uri to write
 */
suspend fun writePlaylist(context: Context, uri: Uri, songs: List<Song>): Boolean =
    openOutputStreamSafe(context, uri, "rwt")?.use { stream ->
        try {
            M3UWriter.write(stream, songs, true)
            delay(250)
            sentPlaylistChangedLocalBoardCast()
            true
        } catch (e: Exception) {
            reportError(e, TAG, "Failed to write $uri")
            false
        }
    } == true


private const val TAG = "PlaylistCreate"
