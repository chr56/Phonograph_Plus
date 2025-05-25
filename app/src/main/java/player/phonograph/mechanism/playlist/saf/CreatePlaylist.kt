/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist.saf

import player.phonograph.App
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.event.EventHub
import player.phonograph.mechanism.playlist.m3u.M3UWriter
import player.phonograph.model.Song
import player.phonograph.util.openOutputStreamSafe
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
            EventHub.sendEvent(App.instance, EventHub.EVENT_PLAYLISTS_CHANGED)
            true
        } catch (e: Exception) {
            warning(context, TAG, "Failed to write $uri", e)
            false
        }
    } == true


private const val TAG = "PlaylistCreate"
