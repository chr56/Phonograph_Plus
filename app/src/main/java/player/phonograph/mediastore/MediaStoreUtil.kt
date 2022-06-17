/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.MediaStore.MediaColumns.DATA
import android.util.Log
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlinx.coroutines.CoroutineScope
import player.phonograph.R
import player.phonograph.model.FileEntity
import player.phonograph.model.Location
import player.phonograph.model.Song
import player.phonograph.settings.Setting

object MediaStoreUtil {
    private const val TAG: String = "MediaStoreUtil"

    /* ***************************
     **           Songs          **
     *****************************/

    fun getAllSongs(context: Context): List<Song> {
        val cursor = querySongs(context, null, null)
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
                songs.add(getSongFromCursor(cursor))
            } while (cursor.moveToNext())
            cursor.close()
        }
        return songs
    }

    fun getSong(cursor: Cursor?): Song {
        if (cursor != null) {
            val song: Song =
                if (cursor.moveToFirst()) {
                    getSongFromCursor(cursor)
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
                getSongFromCursor(cursor)
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
        queryFiles(
            context,
            "$DATA LIKE ?",
            arrayOf("${currentLocation.absolutePath}%"),
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val list: MutableList<FileEntity> = ArrayList()
                do {
                    val id = cursor.getLong(cursor.getColumnIndex(AudioColumns._ID))
                    val displayName = cursor.getString(cursor.getColumnIndex(AudioColumns.DISPLAY_NAME))
                    val absolutePath = cursor.getString(cursor.getColumnIndex(AudioColumns.DATA))
                    val size = cursor.getLong(cursor.getColumnIndex(AudioColumns.SIZE))
                    val dateAdded = cursor.getLong(cursor.getColumnIndex(AudioColumns.DATE_ADDED))
                    val dateModified = cursor.getLong(cursor.getColumnIndex(AudioColumns.DATE_MODIFIED))

                    val songRelativePath = absolutePath.substringAfter(currentLocation.absolutePath).removePrefix("/")
                    val basePath = currentLocation.basePath.let { if (it == "/") "" else it } // root //todo

                    val item = if (songRelativePath.contains('/')) {
                        val folderName = songRelativePath.substringBefore('/')
                        // folder
                        FileEntity.Folder(
                            location = currentLocation.changeTo("$basePath/$folderName"),
                            name = folderName,
                        )
                    } else {
                        // file
                        FileEntity.File(
                            location = currentLocation.changeTo("$basePath/$songRelativePath"),
                            name = displayName,
                            id = id,
                            size = size,
                            dateAdded = dateAdded,
                            dateModified = dateModified
                        )
                    }

                    list.put(item)
                } while (cursor.moveToNext())
                return list.toSortedSet()
            }
        }
        return null
    }

    internal fun MutableList<FileEntity>.put(item: FileEntity) {
        when (item) {
            is FileEntity.File -> {
                this.add(item)
            }
            is FileEntity.Folder -> {
                // count songs for folder
                val i = this.indexOf(item)
                if (i < 0) {
                    this.add(item.apply { songCount = 1 })
                } else {
                    (this[i] as FileEntity.Folder).songCount ++
                }
            }
        }
    }

    private fun getSongFromCursor(cursor: Cursor): Song {
        val id = cursor.getLong(0)
        val title = cursor.getString(1)
        val trackNumber = cursor.getInt(2)
        val year = cursor.getInt(3)
        val duration = cursor.getLong(4)
        val data = cursor.getString(5)
        val dateAdded = cursor.getLong(6)
        val dateModified = cursor.getLong(7)
        val albumId = cursor.getLong(8)
        val albumName = cursor.getString(9)
        val artistId = cursor.getLong(10)
        val artistName = cursor.getString(11)
        return Song(
            id = id,
            title = title,
            trackNumber = trackNumber,
            year = year,
            duration = duration,
            data = data,
            dateAdded = dateAdded,
            dateModified = dateModified,
            albumId = albumId,
            albumName = albumName,
            artistId = artistId,
            artistName = artistName
        )
    }

    /**
     * query audio file via MediaStore
     */
    @JvmOverloads
    fun querySongs(
        context: Context,
        selection: String?,
        selectionValues: Array<String>?,
        sortOrder: String? = Setting.instance.songSortMode.SQLQuerySortOrder,
    ): Cursor? {

        val (realSelection, realSelectionValues) =
            Pair(
                first = if ((selection ?: "").trim { it <= ' ' } != "") {
                    "${SongConst.BASE_AUDIO_SELECTION} AND $selection "
                } else {
                    SongConst.BASE_AUDIO_SELECTION
                },
                second = selectionValues ?: emptyArray()
            ).generateBlacklistFilter(context)

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                SongConst.BASE_SONG_PROJECTION, realSelection, realSelectionValues, sortOrder
            )
        } catch (e: SecurityException) {
        }

        return cursor
    }

    fun queryFiles(
        context: Context,
        selection: String = "",
        selectionValues: Array<String> = emptyArray(),
    ): Cursor? {
        val (realSelection, realSelectionValues) =
            Pair(
                first = if (selection.trim { it <= ' ' } != "") {
                    "${SongConst.BASE_AUDIO_SELECTION} AND $selection "
                } else {
                    SongConst.BASE_AUDIO_SELECTION
                },
                second = selectionValues
            ).generateBlacklistFilter(context)

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                SongConst.BASE_FILE_PROJECTION, realSelection, realSelectionValues, null
            )
        } catch (e: SecurityException) {
        }

        return cursor
    }

    /**
     * delete songs by path via MediaStore
     */
    fun deleteSongs(context: Activity, songs: List<Song>) {

        val total: Int = songs.size
        var result: Int = 0
        val failList: MutableList<Song> = ArrayList<Song>()

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

        // handle fail , report and try again
        if (failList.isNotEmpty()) {
            val list = StringBuffer()
            for (song in failList) {
                list.append(song.title).append("\n")
            }
            MaterialDialog(context)
                .title(R.string.failed_to_delete)
                .message(
                    text = "${context.resources.getQuantityString(R.plurals.msg_deletion_result, total, result, total)}\n" +
                        "${context.getString(R.string.failed_to_delete)}: \n" +
                        "$list "
                )
                .positiveButton(android.R.string.ok)
                .neutralButton(R.string.retry) {
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

    /**
     * Const values about MediaStore of Audio
     */
    object SongConst {
        // just select only songs
        const val BASE_AUDIO_SELECTION =
            "${AudioColumns.IS_MUSIC} =1 AND ${AudioColumns.TITLE} != '' "

        val BASE_SONG_PROJECTION = arrayOf(
            BaseColumns._ID, // 0
            AudioColumns.TITLE, // 1
            AudioColumns.TRACK, // 2
            AudioColumns.YEAR, // 3
            AudioColumns.DURATION, // 4
            AudioColumns.DATA, // 5
            AudioColumns.DATE_ADDED, // 6
            AudioColumns.DATE_MODIFIED, // 7
            AudioColumns.ALBUM_ID, // 8
            AudioColumns.ALBUM, // 9
            AudioColumns.ARTIST_ID, // 10
            AudioColumns.ARTIST, // 11
        )

        val BASE_FILE_PROJECTION = arrayOf(
            BaseColumns._ID, // 0
            AudioColumns.DISPLAY_NAME, // 1
            AudioColumns.DATA, // 2
            AudioColumns.SIZE, // 3
            AudioColumns.DATE_ADDED, // 4
            AudioColumns.DATE_MODIFIED, // 5
        )
    }
}
