/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.MediaStore.MediaColumns.DATA
import android.widget.Toast
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
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

    fun FileEntity.File.linkedSong(context: Context): Song = MediaStoreUtil.getSong(context, id)


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



}
