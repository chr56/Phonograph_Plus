/*
 * Copyright (c) 2021 chr_56
 */

package com.kabouzeid.gramophone.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.model.Song
import java.util.Locale

object MediaStoreUtil {
    private const val TAG: String = "MediaStoreUtil"

    /**
     * delete songs by path via MediaStore
     */
    fun deleteSongs(context: Context, songs: List<Song>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (context.checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(context, R.string.permission_external_storage_denied, Toast.LENGTH_SHORT).show()
                Log.w(TAG, "No MANAGE_EXTERNAL_STORAGE permission")

                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
//                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                Handler().postDelayed({context.startActivity(intent)}, 2200)

            }
        }

        val total: Int = songs.size
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
        if (failList.isNotEmpty()) {
            val list = StringBuffer()
            for (song in failList) {
                list.append(song.title).append("\n")
            }
            MaterialDialog(context)
                .title(R.string.failed_to_delete)
                .message(text = "${context.resources.getQuantityString(R.plurals.msg_deletion_result,total,result,total)}\n" +
                        "${context.getString(R.string.failed_to_delete)}: \n" +
                        "$list ")
                .positiveButton(android.R.string.ok)
                .show()
        }


        Toast.makeText(
            context,
            String.format(Locale.getDefault(), context.getString(R.string.deleted_x_songs), result),
            Toast.LENGTH_SHORT
        ).show()
    }
}
