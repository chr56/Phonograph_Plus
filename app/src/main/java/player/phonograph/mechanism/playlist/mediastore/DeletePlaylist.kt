/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.playlist.mediastore

import legacy.phonograph.MediaStoreCompat
import player.phonograph.R
import player.phonograph.util.coroutineToast
import player.phonograph.util.sentPlaylistChangedLocalBoardCast
import player.phonograph.util.warning
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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
            MediaStoreCompat.Audio.Playlists.EXTERNAL_CONTENT_URI,
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

    sentPlaylistChangedLocalBoardCast()
    failList.toLongArray()
}

private const val TAG = "DeletePlaylist"
