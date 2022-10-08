/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.mediastore

import android.app.Activity
import android.content.Context
import android.provider.MediaStore.Audio
import android.util.Log
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.model.Song


object DeleteSongUtil {

    /**
     * delete songs by path via MediaStore
     * @return fail list
     */
    fun deleteSongs(context: Activity, songs: List<Song>): List<Song> {

        var sucesss = 0
        val failList: MutableList<Song> = ArrayList()

        // try to delete
        for (index in songs.indices) {
            val song = songs[index]
            val result = deleteViaMediaStore(context, song)
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
    private fun deleteViaMediaStore(context: Context, song: Song): Boolean {
        val output =
            context.contentResolver.delete(
                Audio.Media.EXTERNAL_CONTENT_URI,
                "${Audio.Media.DATA} = ?",
                arrayOf(song.data)
            )
        // if it failed
        return if (output <= 0) {
            if (DEBUG) Log.w(TAG, "fail to delete ${song.title}(${song.data})")
            false
        } else {
            true
        }
    }

    private const val TAG = "DeletionUtil"
}