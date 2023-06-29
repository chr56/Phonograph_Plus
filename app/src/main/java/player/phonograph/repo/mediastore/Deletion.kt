/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.Song
import player.phonograph.util.debug
import android.app.Activity
import android.content.Context
import android.provider.MediaStore.Audio
import android.util.Log

/**
 * delete songs by path via MediaStore
 * @return failed-to-delete list
 */
fun deleteSongsViaMediaStore(context: Activity, songs: List<Song>): List<Song> {

    var sucesss = 0
    val failList = mutableListOf<Song>()

    // try to delete
    for (index in songs.indices) {
        val song = songs[index]
        val result = deleteViaMediaStoreImpl(context, song)
        if (result) {
            sucesss += 1
        } else {
            failList.add(song)
        }
    }
    return failList
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
