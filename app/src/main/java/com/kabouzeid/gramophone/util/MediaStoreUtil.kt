package com.kabouzeid.gramophone.util

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.model.Song
import java.util.Locale

object MediaStoreUtil {
    const val TAG: String = "MediaStoreUtil"

    fun deleteSongs(context: Context, songs: List<Song>) {

        var total: Int = songs.size
        var result: Int = 0
        val failList: MutableList<Song> = ArrayList<Song>()

        for (index in songs.indices) {
            val output = context.contentResolver.delete(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Audio.Media.DATA} = ?",
                arrayOf(songs[index].data)
            )
            if (output == 0) {
                // if it failed
                Log.w(TAG, "fail to delete song ${songs[index].title} at ${songs[index].data}")
                failList.add(songs[index])
            }
            result += output
        }
        // report fail
        if (failList.isNotEmpty()){
            val buffer = StringBuffer()
            for (song in failList){
                buffer.append(song.title).append(",\n")
            }
            val dialog = MaterialDialog(context)
                .title(R.string.failed_to_delete)
                .message(text = "ERROR\n ${buffer.toString()} are failed to delete")
                .positiveButton(android.R.string.ok)
        }

        /*
        val path: Array<String> = Array(songs.size) { songs[it].data }
        var where: String = ""
        for (i in songs.indices) {
            if (i > 0) where += " OR "
            where += "${MediaStore.Audio.Media.DATA} = ?"
        }
        Log.d("MSU", "where = '$where' ")
        val result: Int = context.contentResolver.delete(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, where, path
        )
         */

        Toast.makeText(
            context,
            String.format(Locale.getDefault(), context.getString(R.string.deleted_x_songs), result),
            Toast.LENGTH_SHORT
        ).show()
    }
}
