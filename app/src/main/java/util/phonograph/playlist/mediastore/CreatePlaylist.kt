/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.mediastore

import legacy.phonograph.MediaStoreCompat
import legacy.phonograph.MediaStoreCompat.Audio.Playlists.EXTERNAL_CONTENT_URI
import legacy.phonograph.MediaStoreCompat.Audio.PlaylistsColumns
import player.phonograph.R
import player.phonograph.util.coroutineToast
import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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