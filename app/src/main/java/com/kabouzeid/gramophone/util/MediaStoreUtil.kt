/*
 * Copyright (c) 2021 chr_56
 */

package com.kabouzeid.gramophone.util

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
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
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.indices
import kotlin.collections.isNotEmpty

object MediaStoreUtil {
    private const val TAG: String = "MediaStoreUtil"

    /**
     * delete songs by path via MediaStore
     */
    fun deleteSongs(context: Activity, songs: List<Song>) {
        // permission check on R
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (context.checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
//                Toast.makeText(context, R.string.permission_external_storage_denied, Toast.LENGTH_SHORT).show()
                MaterialDialog(context)
                    .title(R.string.permission_external_storage_denied)
                    .message(R.string.permission_external_storage_denied)
                    .positiveButton(text = "Request Permission") {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
//                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        Handler().postDelayed({ context.startActivity(intent) }, 200)
                    }
                    .negativeButton(android.R.string.cancel)
                    .show()
                Log.w(TAG, "No MANAGE_EXTERNAL_STORAGE permission")
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
                .message(
                    text = "${context.resources.getQuantityString(R.plurals.msg_deletion_result,total,result,total)}\n" +
                        "${context.getString(R.string.failed_to_delete)}: \n" +
                        "$list "
                )
                .positiveButton(android.R.string.ok)
                .neutralButton(text = "Retry") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val uris: List<Uri> = List<Uri>(failList.size) { index ->
                            Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, failList[index].id.toString())
                        }
                        uris.forEach {
                            Log.d(TAG, it.toString())
                            Log.d(TAG, it.path.toString())
                        }
                        val pi: PendingIntent = MediaStore.createDeleteRequest(
                            context.contentResolver, uris
                        )
                        context.startIntentSenderForResult(pi.intentSender, 0, null, 0, 0, 0)
                    } else {
                        // todo
                    }
                }
                .show()
        }

        Toast.makeText(
            context,
            String.format(Locale.getDefault(), context.getString(R.string.deleted_x_songs), result),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun scanFiles(context: Context, paths: Array<String>, mimeTypes: Array<String>) {
        var failed: Int = 0
        var scanned: Int = 0
        MediaScannerConnection.scanFile(context, paths, mimeTypes) { _: String?, uri: Uri? ->
            if (uri == null) {
                failed++
            } else {
                scanned++
            }
            val text = "${context.resources.getString(R.string.scanned_files, scanned, scanned + failed)} ${
            if (failed > 0) String.format(context.resources.getString(R.string.could_not_scan_files, failed)) else ""}"
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}
