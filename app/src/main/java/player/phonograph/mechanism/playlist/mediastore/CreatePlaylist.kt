/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist.mediastore

import legacy.phonograph.MediaStoreCompat
import legacy.phonograph.MediaStoreCompat.Audio.Playlists.EXTERNAL_CONTENT_URI
import legacy.phonograph.MediaStoreCompat.Audio.PlaylistsColumns
import player.phonograph.R
import player.phonograph.mechanism.playlist.m3u.M3UWriter
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.util.coroutineToast
import player.phonograph.util.sentPlaylistChangedLocalBoardCast
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffix
import player.phonograph.util.warning
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

suspend fun createPlaylistViaMediastore(context: Context, name: String, songs: List<Song>) {
    val id = createOrFindPlaylistViaMediastore(context, name)
    if (PlaylistLoader.checkExistence(context, id)) {
        addToPlaylistViaMediastore(context, songs, id, true)
        coroutineToast(context, R.string.success)
        delay(250)
        sentPlaylistChangedLocalBoardCast()
    } else {
        warning("Playlist", "Failed to save playlist (id=$id)")
        coroutineToast(context, R.string.failed)
    }
}


/**
 * find or create playlist via MediaStore
 * @return playlist id created or found (-1 if failed)
 */
suspend fun createOrFindPlaylistViaMediastore(
    context: Context,
    name: String,
): Long = withContext(Dispatchers.IO) {
    if (name.isNotEmpty()) {
        // query first
        val cursor = context.contentResolver.query(
            EXTERNAL_CONTENT_URI,
            arrayOf(MediaStoreCompat.Audio.Playlists._ID/* 0 */),
            PlaylistsColumns.NAME + "=?", arrayOf(name), null
        )
        if (cursor == null) {
            createPlaylistImpl(context, name)
        } else if (cursor.count < 1) {
            cursor.close()
            createPlaylistImpl(context, name)
        } else {
            // Playlist exists
            cursor.use {
                if (it.moveToFirst()) {
                    it.getLong(0)
                } else {
                    -1
                }
            }
        }
    } else {
        coroutineToast(context, R.string.could_not_create_playlist)
        -1
    }
}

/**
 * @return playlist id created (-1 if failed)
 */
private suspend fun createPlaylistImpl(context: Context, name: String): Long {
    val values = ContentValues(1).apply {
        put(PlaylistsColumns.NAME, name)
    }
    val uri = context.contentResolver.insert(EXTERNAL_CONTENT_URI, values)
    return if (uri != null) {
        // Necessary because somehow the MediaStoreObserver doesn't work for playlists
        context.contentResolver.notifyChange(uri, null)
        coroutineToast(context, context.resources.getString(R.string.created_playlist_x, name))
        uri.lastPathSegment?.toLong() ?: -1
    } else {
        coroutineToast(context, R.string.could_not_create_playlist)
        -1
    }
}

suspend fun duplicatePlaylistViaMediaStore(context: Context, playlists: List<Playlist>) {
    var successes = 0
    var failures = 0
    var dir: String? = ""
    val failureList = StringBuffer()
    for (playlist in playlists) {
        try {
            val filename: String =
                if (playlist is SmartPlaylist) {
                    // Since AbsCustomPlaylists are dynamic, we add a timestamp after their names.
                    playlist.name + dateTimeSuffix(currentDate())
                } else {
                    playlist.name
                }
            val songs = playlist.getSongs(context)
            dir = M3UWriter.write(File(Environment.DIRECTORY_DOWNLOADS), songs, filename).parent
            successes++
        } catch (e: IOException) {
            failures++
            failureList.append(playlist.name).append(" ")
            Log.e("Playlist", "Failed to write playlist", e)
        }
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