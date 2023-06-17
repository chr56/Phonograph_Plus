/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.mediastore

import legacy.phonograph.MediaStoreCompat
import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import player.phonograph.R
import player.phonograph.model.PlaylistSong
import player.phonograph.model.Song
import player.phonograph.util.coroutineToast
import player.phonograph.mechanism.PlaylistsManagement
import player.phonograph.mediastore.PlaylistLoader
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @param id playlist id to rename
 * @param newName new name
 * @return success or not
 */
suspend fun renamePlaylistViaMediastore(
    context: Context,
    id: Long,
    newName: String,
): Boolean = withContext(Dispatchers.IO) {
    val playlistUri = PlaylistsManagement.getPlaylistUris(id)
    try {
        val result = context.contentResolver.update(playlistUri, ContentValues().apply {
            put(MediaStoreCompat.Audio.PlaylistsColumns.NAME, newName)
        }, null, null)
        if (result > 0) {
            // Necessary because somehow the MediaStoreObserver doesn't work for playlists
            context.contentResolver.notifyChange(playlistUri, null)
            coroutineToast(context, R.string.success)
            true
        } else {
            coroutineToast(context, R.string.failed)
            false
        }
    } catch (ignored: Exception) {
        coroutineToast(context, R.string.failed)
        false
    }
}

/**
 * @return success or not
 */
suspend fun addToPlaylistViaMediastore(
    context: Context,
    song: Song,
    playlistId: Long,
    showToastOnFinish: Boolean,
): Boolean =
    addToPlaylistViaMediastore(context, listOf(song), playlistId, showToastOnFinish)

/**
 * @return success or not
 */
suspend fun addToPlaylistViaMediastore(
    context: Context,
    songs: List<Song>,
    playlistId: Long,
    showToastOnFinish: Boolean,
): Boolean = withContext(Dispatchers.IO) {
    val uri = Playlists.Members.getContentUri("external", playlistId)
    var cursor: Cursor? = null
    var base = 0
    try {
        try {
            val projection =
                arrayOf("max(" + Playlists.Members.PLAY_ORDER + ")")
            cursor = context.contentResolver
                .query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                base = cursor.getInt(0) + 1
            }
        } finally {
            cursor?.close()
        }

        var numInserted = 0
        var offSet = 0
        while (offSet < songs.size) {
            numInserted += context.contentResolver.bulkInsert(
                uri, makeInsertItemsViaMediastore(songs, offSet, 1000, base)
            )
            offSet += 1000
        }

        if (showToastOnFinish) {
            coroutineToast(
                context,
                context.resources.getString(
                    R.string.inserted_x_songs_into_playlist_x, numInserted,
                    PlaylistLoader.playlistId(context, playlistId).name
                ),
            )
        }
        // Necessary because somehow the MediaStoreObserver doesn't work for playlists
        context.contentResolver.notifyChange(uri, null)
        true
    } catch (ignored: SecurityException) {
        false
    }
}

private fun makeInsertItemsViaMediastore(
    songs: List<Song>,
    offset: Int,
    lenth: Int,
    base: Int,
): Array<ContentValues?> {
    var len = lenth
    if (offset + len > songs.size) {
        len = songs.size - offset
    }
    val contentValues = arrayOfNulls<ContentValues>(len)
    for (i in 0 until len) {
        contentValues[i] = ContentValues().apply {
            put(Playlists.Members.PLAY_ORDER, base + offset + i)
            put(Playlists.Members.AUDIO_ID, songs[offset + i].id)
        }
    }
    return contentValues
}

suspend fun moveItemViaMediastore(
    context: Context,
    playlistId: Long,
    from: Int,
    to: Int,
): Boolean = withContext(Dispatchers.IO) {
    val res = Playlists.Members.moveItem(context.contentResolver, playlistId, from, to)
    //// Necessary because somehow the MediaStoreObserver doesn't work for playlists
    //// NOTE: actually for now lets disable this because it messes with the animation (tested on Android 11)
    // context.contentResolver.notifyChange(getPlaylistUris(context, playlistId), null)
    res
}

/**
 * @return success or not
 */
suspend fun removeFromPlaylistViaMediastore(
    context: Context,
    song: Song,
    playlistId: Long,
): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.delete(
            /* url = */ if (Build.VERSION.SDK_INT >= 29) {
                Playlists.Members.getContentUri(
                    MediaStore.getExternalVolumeNames(context).firstOrNull(), playlistId
                )
            } else {
                PlaylistsManagement.getPlaylistUris(playlistId)
            },
            /* where = */ Playlists.Members.AUDIO_ID + " =?",
            /* selectionArgs = */ arrayOf(song.id.toString())
        )
        // Necessary because somehow the MediaStoreObserver doesn't work for playlists
        context.contentResolver.notifyChange(PlaylistsManagement.getPlaylistUris(playlistId), null)
        true
    } catch (ignored: SecurityException) {
        false
    }
}

/**
 * @return success or not
 */
suspend fun removeFromPlaylistViaMediastore(
    context: Context,
    songs: List<PlaylistSong>,
): Boolean = withContext(Dispatchers.IO) {
    val selectionArgs = arrayOfNulls<String>(songs.size)
    for (i in selectionArgs.indices) {
        selectionArgs[i] = songs[i].idInPlayList.toString()
    }

    var selection = Playlists.Members._ID + " IN ("
    for (selectionArg in selectionArgs) selection += "?, "
    selection = selection.substring(0, selection.length - 2) + ")"

    try {
        if (Build.VERSION.SDK_INT >= 29)
            context.contentResolver.delete(
                Playlists.Members.getContentUri(
                    MediaStore.getExternalVolumeNames(context).firstOrNull(),
                    songs[0].playlistId
                ),
                selection, selectionArgs
            )
        else
            context.contentResolver.delete(
                PlaylistsManagement.getPlaylistUris(songs[0].playlistId),
                selection,
                selectionArgs
            )
        // Necessary because somehow the MediaStoreObserver is not notified when adding a playlist
        context.contentResolver.notifyChange(
            PlaylistsManagement.getPlaylistUris(songs[0].playlistId),
            null
        )
        true
    } catch (ignored: SecurityException) {
        false
    }
}