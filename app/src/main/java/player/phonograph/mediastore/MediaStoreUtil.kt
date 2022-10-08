/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.MediaStore.MediaColumns.DATA
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.model.file.put
import player.phonograph.util.PermissionUtil.navigateToStorageSetting
import player.phonograph.util.Util
import java.io.File

object MediaStoreUtil {
    private const val TAG: String = "MediaStoreUtil"

    /* ***************************
     **           Songs          **
     *****************************/

    fun getAllSongs(context: Context): List<Song> {
        val cursor = querySongs(context)
        return getSongs(cursor)
    }

    fun getSongs(context: Context, title: String): List<Song> {
        val cursor = querySongs(
            context, "${AudioColumns.TITLE} LIKE ?", arrayOf("%$title%")
        )
        return getSongs(cursor)
    }

    fun getSong(context: Context, id: Long): Song {
        val cursor =
            querySongs(
                context, "${AudioColumns._ID} =? ", arrayOf(id.toString())
            )
        return getSong(cursor)
    }

    fun getSongs(cursor: Cursor?): List<Song> {
        val songs: MutableList<Song> = ArrayList()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                songs.add(parseSong(cursor))
            } while (cursor.moveToNext())
            cursor.close()
        }
        return songs
    }

    fun getSong(cursor: Cursor?): Song {
        if (cursor != null) {
            val song: Song =
                if (cursor.moveToFirst()) {
                    parseSong(cursor)
                } else {
                    Song.EMPTY_SONG
                }
            cursor.close()
            return song
        }
        return Song.EMPTY_SONG
    }

    fun getSong(context: Context, file: File): Song? {
        return querySongs(context, "$DATA LIKE ?", arrayOf(file.path))?.use { cursor ->
            if (cursor.moveToFirst()) {
                parseSong(cursor)
            } else null
        }
    }

    /**
     * This might be time-consuming
     * @param currentLocation the location you want to query
     * @param scope CoroutineScope (Optional)
     * @return the ordered TreeSet containing songs and folders in this location
     */
    @SuppressLint("Range") // todo
    fun searchSongFiles(context: Context, currentLocation: Location, scope: CoroutineScope? = null): Set<FileEntity>? {
        val fileCursor = querySongFiles(
            context,
            "$DATA LIKE ?",
            arrayOf("${currentLocation.absolutePath}%"),
        ) ?: return null
        return fileCursor.use { cursor ->
            if (cursor.moveToFirst()) {
                val list: MutableList<FileEntity> = ArrayList()
                do {
                    val item = parseFileEntity(cursor, currentLocation)
                    list.put(item)
                } while (cursor.moveToNext())
                list.toSortedSet()
            } else null
        }
    }

    fun searchSongs(context: Context, currentLocation: Location, scope: CoroutineScope? = null): List<Song> {
        val cursor = querySongs(
            context, "$DATA LIKE ?", arrayOf("%${currentLocation.absolutePath}%")
        )
        return getSongs(cursor)
    }

    fun FileEntity.File.linkedSong(context: Context): Song = MediaStoreUtil.getSong(context, id)


    /**
     * delete songs by path via MediaStore
     */
    fun deleteSongs(context: Activity, songs: List<Song>) {
        val total = songs.size
        var result = 0
        val failList: MutableList<Song> = ArrayList()

        // try to delete
        for (index in songs.indices) {
            val output = context.contentResolver.delete(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Audio.Media.DATA} = ?",
                arrayOf(songs[index].data)
            )
            // if it failed
            if (output == 0) {
                Log.w(TAG, "fail to delete song ${songs[index].title} at ${songs[index].data}")
                failList.add(songs[index])
            }
            result += output
        }

        val r: String = context.resources.getQuantityString(R.plurals.msg_deletion_result, total, result, total)

        Util.withLooper {
            Toast.makeText(context, r, Toast.LENGTH_SHORT).show()
        }

        // handle fail , report and try again
        // handle fail , report and try again
        if (failList.isNotEmpty()) AlertDialog.Builder(context).apply {
            setTitle(R.string.failed_to_delete)
            setMessage(
                "${r}\n${context.getString(R.string.failed_to_delete)}: \n" +
                        "${failList.fold("") { acc, song -> "$acc,${song.title}" }} ")
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            setNegativeButton(R.string.grant_permission) { _, _ ->
                navigateToStorageSetting(context)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setNeutralButton(R.string.retry) { _, _ ->
                    val uris = failList.map { song ->
                        Uri.withAppendedPath(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            song.id.toString()
                        )
                    }
                    context.startIntentSenderForResult(
                        MediaStore.createDeleteRequest(
                            context.contentResolver,
                            uris
                        ).intentSender,
                        0, null, 0, 0, 0)
                }
            }
        }
            .show()
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
                if (failed > 0) String.format(context.resources.getString(R.string.could_not_scan_files, failed)) else ""
            }"
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    fun searchSong(context: Context, fileName: String): Song {
        val cursor = querySongs(
            context,
            selection = "$DATA LIKE ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? ",
            selectionValues = arrayOf(fileName, fileName)
        )
        return getSong(cursor)
    }

}
