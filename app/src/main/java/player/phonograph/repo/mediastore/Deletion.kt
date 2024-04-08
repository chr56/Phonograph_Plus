/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.Song
import player.phonograph.util.debug
import android.content.Context
import android.provider.MediaStore.Audio
import android.util.Log

/**
 * delete songs by path via MediaStore
 * @return failed-to-delete list
 */
fun deleteSongsViaMediaStore(context: Context, songs: Collection<Song>): List<Song> {
    return songs.filter { song -> !deleteViaMediaStoreImpl(context, song) }
}

/**
 * delete song by path via MediaStore
 * @return success or not
 */
fun deleteSongViaMediaStore(context: Context, song: Song): Boolean {
    return deleteViaMediaStoreImpl(context, song)
}

/**
 * @return success or not
 */
private fun deleteViaMediaStoreImpl(context: Context, song: Song): Boolean {
    val output = context.contentResolver.delete(
        Audio.Media.EXTERNAL_CONTENT_URI, "${Audio.Media.DATA} = ?", arrayOf(song.data)
    )
    // if it failed
    return if (output <= 0) {
        debug {
            Log.w(TAG, "fail to delete ${song.title}(${song.data})")
        }
        false
    } else {
        true
    }
}

private const val TAG = "DeleteSongs"
