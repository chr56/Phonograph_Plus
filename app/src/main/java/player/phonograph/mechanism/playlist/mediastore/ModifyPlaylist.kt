/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist.mediastore

import legacy.phonograph.MediaStoreCompat
import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import player.phonograph.App
import player.phonograph.R
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.util.concurrent.coroutineToast
import player.phonograph.util.mediastoreUriPlaylist
import player.phonograph.util.mediastoreUriPlaylistMembers
import player.phonograph.util.mediastoreUriPlaylists
import player.phonograph.util.reportError
import player.phonograph.util.warning
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @param volume Mediastore volume where playlist is in
 * @param id playlist id to rename
 * @param newName new name
 * @return success or not
 */
suspend fun renamePlaylistViaMediastore(
    context: Context,
    volume: String,
    id: Long,
    newName: String,
): Boolean = withContext(Dispatchers.IO) {
    val playlistUri = mediastoreUriPlaylist(volume, id)
    try {
        val result = context.contentResolver.update(playlistUri, ContentValues().apply {
            put(MediaStoreCompat.Audio.PlaylistsColumns.NAME, newName)
        }, null, null)
        if (result > 0) {
            // Necessary because somehow the MediaStoreObserver doesn't work for playlists
            context.contentResolver.notifyChange(playlistUri, null)
            true
        } else {
            false
        }
    } catch (ignored: Exception) {
        false
    }
}

/**
 * @return success or not
 */
suspend fun addToPlaylistViaMediastore(
    context: Context,
    song: Song,
    volume: String,
    playlistId: Long,
    showToastOnFinish: Boolean,
): Boolean =
    addToPlaylistViaMediastore(context, listOf(song), volume, playlistId, showToastOnFinish)

/**
 * @return success or not
 */
suspend fun addToPlaylistViaMediastore(
    context: Context,
    songs: List<Song>,
    volume: String,
    playlistId: Long,
    showToastOnFinish: Boolean,
): Boolean = withContext(Dispatchers.IO) {
    val uri = mediastoreUriPlaylistMembers(volume, playlistId)
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
                    PlaylistLoader.id(context, playlistId)?.name.orEmpty()
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
    try {
        val res = Playlists.Members.moveItem(context.contentResolver, playlistId, from, to)
        //// Necessary because somehow the MediaStoreObserver doesn't work for playlists
        //// NOTE: actually for now lets disable this because it messes with the animation (tested on Android 11)
        // context.contentResolver.notifyChange(getPlaylistUris(context, playlistId), null)
        res
    } catch (e: Exception) {
        reportError(e, TAG, "Failed to reorder playlist ($playlistId: $from -> $to)")
        false
    }
}

/**
 * delete playlists by ids via MediaStore
 * @return playlist ids failing to delete
 */
suspend fun deletePlaylistsViaMediastore(
    context: Context,
    playlistIds: LongArray,
): LongArray = withContext(Dispatchers.IO) {
    var success = 0
    val failList = mutableListOf<Long>()
    // try to delete
    for (id in playlistIds) {
        val result = context.contentResolver.delete(
            mediastoreUriPlaylists(MEDIASTORE_VOLUME_EXTERNAL),
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(id.toString())
        )
        if (result == 0) {
            Log.w(TAG, "failed to delete playlist id: $id")
            failList.add(id)
        }
        success += result
    }
    coroutineToast(
        context,
        context.resources.getQuantityString(R.plurals.msg_deletion_result, playlistIds.size, success, playlistIds.size)
    )
    if (failList.isNotEmpty())
        warning(TAG, failList.fold("Failed to delete playlist(id):") { acc, s -> "$acc, $s" })

    EventHub.sendEvent(App.instance, EventHub.EVENT_PLAYLISTS_CHANGED)
    failList.toLongArray()
}

/**
 * @return success or not
 */
suspend fun removeFromPlaylistViaMediastore(
    context: Context,
    volume: String,
    playlistId: Long,
    songId: Long,
    index: Long,
): Int = withContext(Dispatchers.IO) {
    try {
        val playlistUri = mediastoreUriPlaylistMembers(volume, playlistId)
        val deleted = context.contentResolver.delete(
            playlistUri,
            "${Playlists.Members.AUDIO_ID} = ? AND ${Playlists.Members.PLAY_ORDER} = ?",
            arrayOf(songId.toString(), (index + 1).toString()) // start with 1
        )
        // Necessary because somehow the MediaStoreObserver doesn't work for playlists
        context.contentResolver.notifyChange(playlistUri, null)
        if (deleted > 1) Log.e(TAG, "More items have been deleted!")
        deleted
    } catch (ignored: SecurityException) {
        0
    }
}

private const val TAG = "Playlist"