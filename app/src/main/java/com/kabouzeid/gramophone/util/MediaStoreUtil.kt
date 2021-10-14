package com.kabouzeid.gramophone.util

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.model.Song
import java.util.Locale

object MediaStoreUtil {

    fun deleteSongs(context: Context, songs: List<Song>) {
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

        Toast.makeText(
            context,
            String.format(Locale.getDefault(), context.getString(R.string.deleted_x_songs), result),
            Toast.LENGTH_SHORT
        ).show()
    }
}
