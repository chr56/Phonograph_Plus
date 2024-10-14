/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist.mediastore

import legacy.phonograph.MediaStoreCompat.Audio.PlaylistsColumns
import player.phonograph.model.Song
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.util.mediastoreUriPlaylists
import player.phonograph.util.sentPlaylistChangedLocalBoardCast
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffixCompat
import player.phonograph.util.warning
import android.content.ContentValues
import android.content.Context
import android.net.Uri

/**
 * @return created Playlist id
 */
fun createPlaylistViaMediastore(context: Context, name: String): Long {
    val id = insertNewPlaylist(context, name)
    if (id != -1L) {
        sentPlaylistChangedLocalBoardCast()
    }
    return id
}


/**
 * @return created Playlist id
 */
suspend fun createPlaylistViaMediastore(context: Context, name: String, songs: List<Song>): Long {
    val id = insertNewPlaylist(context, name)
    if (id != -1L) {
        addToPlaylistViaMediastore(context, songs, MEDIASTORE_VOLUME_EXTERNAL, id, true)
        sentPlaylistChangedLocalBoardCast()
    }
    return id
}

/**
 * @return playlist uri (-1 if failed)
 */
private fun insertNewPlaylist(context: Context, name: String): Long {
    val playlistUri = insertNewPlaylist(context, MEDIASTORE_VOLUME_EXTERNAL, name)
    return playlistId(playlistUri)
}

/**
 * @return playlist uri (null if failed)
 */
private fun insertNewPlaylist(context: Context, volume: String, name: String): Uri? {
    val values = ContentValues(1).apply {
        put(PlaylistsColumns.NAME, name)
    }
    val playlistsUri = mediastoreUriPlaylists(volume)
    val playlist = context.contentResolver.insert(playlistsUri, values)
    return if (playlist != null) {
        // Necessary because somehow the MediaStoreObserver doesn't work for playlists
        context.contentResolver.notifyChange(playlist, null)
        playlist
    } else {
        null
    }
}

suspend fun duplicatePlaylistViaMediaStore(
    context: Context,
    songBatches: List<List<Song>>,
    names: List<String>,
): Boolean {
    var successes = 0
    var failures = 0
    val failureList = StringBuffer()
    for ((index, songs) in songBatches.withIndex()) {
        val filename = "${names[index]}_${dateTimeSuffixCompat(currentDate())}"
        val id = insertNewPlaylist(context, filename)
        if (id != -1L) {
            addToPlaylistViaMediastore(context, songs, MEDIASTORE_VOLUME_EXTERNAL, id, true)
            successes++
        } else {
            failures++
            failureList.append(names[index]).append(" ")
        }
    }
    sentPlaylistChangedLocalBoardCast()
    if (failures > 0) {
        warning("Playlist", "Playlists failed to save: $failureList")
    }
    return failures <= 0
}

private fun playlistId(uri: Uri?): Long = uri?.lastPathSegment?.toLong() ?: -1