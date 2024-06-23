/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist.mediastore

import legacy.phonograph.MediaStoreCompat
import legacy.phonograph.MediaStoreCompat.Audio.PlaylistsColumns
import player.phonograph.R
import player.phonograph.mechanism.playlist.m3u.M3UWriter
import player.phonograph.model.Song
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.util.coroutineToast
import player.phonograph.util.sentPlaylistChangedLocalBoardCast
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffixCompat
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * @return created Playlist id
 */
suspend fun createPlaylistViaMediastore(context: Context, name: String): Long {
    val playlistUri = createPlaylistImpl(context, MEDIASTORE_VOLUME_EXTERNAL, name)
    val id = playlistId(playlistUri)
    if (id != -1L) sentPlaylistChangedLocalBoardCast()
    return id
}


/**
 * @return created Playlist id
 */
suspend fun createPlaylistViaMediastore(context: Context, name: String, songs: List<Song>): Long {
    val playlistUri = createPlaylistImpl(context, MEDIASTORE_VOLUME_EXTERNAL, name)
    val id = playlistId(playlistUri)
    if (id != -1L) {
        addToPlaylistViaMediastore(context, songs, MEDIASTORE_VOLUME_EXTERNAL, id, true)
        sentPlaylistChangedLocalBoardCast()
    }
    return id
}

/**
 * @return playlist uri (null if failed)
 */
private fun createPlaylistImpl(context: Context, volume: String, name: String): Uri? {
    val values = ContentValues(1).apply {
        put(PlaylistsColumns.NAME, name)
    }
    val playlistsUri = MediaStoreCompat.Audio.Playlists.getContentUri(volume)
    val playlist = context.contentResolver.insert(playlistsUri, values)
    return if (playlist != null) {
        // Necessary because somehow the MediaStoreObserver doesn't work for playlists
        context.contentResolver.notifyChange(playlist, null)
        playlist
    } else {
        null
    }
}

suspend fun duplicatePlaylistViaMediaStore(context: Context, songBatches: List<List<Song>>, names: List<String>) {
    var successes = 0
    var failures = 0
    var dir: String? = null
    val failureList = StringBuffer()
    for ((index, songs) in songBatches.withIndex()) {
        try {
            val filename: String = "${names[index]}_${dateTimeSuffixCompat(currentDate())}"
            dir = M3UWriter.write(File(Environment.DIRECTORY_DOWNLOADS), songs, filename).parent
            successes++
        } catch (e: IOException) {
            failures++
            failureList.append(names[index]).append(" ")
        }
    }
    if (failures > 0) {
        Log.e("Playlist", "Failed to write playlist: $failureList")
    }
    val msg =
        if (failures == 0) String.format(
            context.applicationContext.getString(R.string.saved_x_playlists_to_x),
            successes, dir
        ) else String.format(
            context.applicationContext.getString(R.string.saved_x_playlists_to_x_failed_to_save_x),
            successes, dir, failures
        )
    coroutineToast(context, msg)
}

private fun playlistId(uri: Uri?): Long = uri?.lastPathSegment?.toLong() ?: -1