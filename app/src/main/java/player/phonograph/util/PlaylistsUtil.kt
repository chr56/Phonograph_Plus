/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package player.phonograph.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Playlists
import android.provider.MediaStore.Audio.PlaylistsColumns
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.documentfile.provider.DocumentFile
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import kotlinx.coroutines.*
import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
import player.phonograph.R
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.model.AbsCustomPlaylist
import player.phonograph.model.Playlist
import player.phonograph.model.PlaylistSong
import player.phonograph.model.Song
import player.phonograph.provider.BlacklistStore
import java.io.*
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object PlaylistsUtil {
    private const val TAG: String = "PlaylistUtil"

    /* ***************************
     **        Playlists         **
     *****************************/

    fun getAllPlaylists(context: Context): List<Playlist> {
        return getAllPlaylists(
            queryPlaylists(context, null, null)
        )
    }
    fun getAllPlaylists(cursor: Cursor?): List<Playlist> {
        val playlists: MutableList<Playlist> = java.util.ArrayList()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                playlists.add(
                    Playlist(cursor.getLong(0), cursor.getString(1))
                )
            } while (cursor.moveToNext())
        }
        cursor?.close()
        return playlists
    }

    fun getPlaylist(context: Context, playlistId: Long): Playlist {
        return getPlaylist(
            queryPlaylists(
                context, BaseColumns._ID + "=?", arrayOf(playlistId.toString())
            )
        )
    }
    fun getPlaylist(context: Context, playlistName: String): Playlist {
        return getPlaylist(
            queryPlaylists(
                context, PlaylistsColumns.NAME + "=?", arrayOf(playlistName)
            )
        )
    }
    fun getPlaylist(cursor: Cursor?): Playlist {
        var playlist = Playlist()
        if (cursor != null && cursor.moveToFirst()) {
            playlist = Playlist(cursor.getLong(0), cursor.getString(1))
        }
        cursor?.close()
        return playlist
    }

    /**
     * query playlist file via MediaStore
     */
    @SuppressLint("Recycle")
    fun queryPlaylists(
        context: Context,
        selection: String?,
        selectionValues: Array<String>?
    ): Cursor? {
        var realSelection =
            if (selection != null && selection.trim { it <= ' ' } != "") {
                "${MediaStoreUtil.SongConst.BASE_PLAYLIST_SELECTION} AND $selection "
            } else {
                MediaStoreUtil.SongConst.BASE_PLAYLIST_SELECTION
            }
        var realSelectionValues = selectionValues

        // Blacklist
        val paths: List<String> = BlacklistStore.getInstance(context).paths
        if (paths.isNotEmpty()) {

            realSelection += " AND ${MediaStore.MediaColumns.DATA} NOT LIKE ?"
            for (i in 0 until paths.size - 1) {
                realSelection += " AND ${MediaStore.MediaColumns.DATA} NOT LIKE ?"
            }
            Log.i(TAG, "playlist selection: $realSelection")

            realSelectionValues =
                Array<String>((selectionValues?.size ?: 0) + paths.size) { index ->
                    // Todo: Check
                    if (index < (selectionValues?.size ?: 0)) selectionValues?.get(index) ?: ""
                    else paths[index - (selectionValues?.size ?: 0)] + "%"
                }
        }

        val cursor: Cursor? = try {
            context.contentResolver.query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                arrayOf(
                    BaseColumns._ID, /* 0 */
                    PlaylistsColumns.NAME /* 1 */
                ),
                realSelection, realSelectionValues, MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER
            )
        } catch (e: SecurityException) {
            null
        }
        return cursor
    }

    fun getPlaylistPath(context: Context, playlist: Playlist): String {
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            arrayOf(
                BaseColumns._ID /* 0 */,
                PlaylistsColumns.NAME /* 1 */,
                PlaylistsColumns.DATA /* 2 */
            ),
            "${BaseColumns._ID} = ? AND ${PlaylistsColumns.NAME} = ?",
            arrayOf(playlist.id.toString(), playlist.name),
            MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER
        )
        var path: String = "-"
        cursor?.let {
            it.moveToFirst()
            path = it.getString(2)
            it.close()
        }
        return path
    }

    /**
     * delete playlist by path via MediaStore
     * @return playlists failing to delete
     */
    fun deletePlaylists(context: Activity, playlists: List<Playlist>): List<Playlist> {
        var result: Int = 0
        val failList: MutableList<Playlist> = ArrayList<Playlist>()
        // try to delete
        for (index in playlists.indices) {
            val output = context.contentResolver.delete(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(playlists[index].id.toString())
            )
            if (output == 0) {
                Log.w(TAG, "fail to delete playlist ${playlists[index].name}(id:${playlists[index].id})")
                failList.add(playlists[index])
            }
            result += output
        }
        Toast.makeText( // todo
            context,
            String.format(Locale.getDefault(), context.getString(R.string.deleted_x_playlists), result),
            Toast.LENGTH_SHORT
        ).show()
        LocalBroadcastManager.getInstance(App.instance).sendBroadcast(Intent(BROADCAST_PLAYLISTS_CHANGED))
        return failList
    }

    fun doesPlaylistExist(context: Context, name: String): Boolean {
        return doesPlaylistExistImp(context, PlaylistsColumns.NAME + "=?", arrayOf(name))
    }
    fun doesPlaylistExist(context: Context, playlistId: Long): Boolean =
        playlistId != -1L && doesPlaylistExistImp(context, Playlists._ID + "=?", arrayOf(playlistId.toString()))

    private fun doesPlaylistExistImp(context: Context, selection: String, values: Array<String>): Boolean {
        val cursor = context.contentResolver
            .query(Playlists.EXTERNAL_CONTENT_URI, arrayOf(), selection, values, null)
        var exists = false
        if (cursor != null) {
            exists = cursor.count != 0
            cursor.close()
        }
        return exists
    }

    @Deprecated("")
    fun createPlaylist(context: Context, name: String): Long {
        var id: Long = -1
        if (name.isNotEmpty()) {
            try {
                val cursor = context.contentResolver.query(
                    Playlists.EXTERNAL_CONTENT_URI,
                    arrayOf(Playlists._ID/* 0 */),
                    PlaylistsColumns.NAME + "=?", arrayOf(name), null
                )
                if (cursor == null || cursor.count < 1) {
                    val values = ContentValues(1)
                    values.put(PlaylistsColumns.NAME, name)
                    val uri = context.contentResolver.insert(
                        Playlists.EXTERNAL_CONTENT_URI, values
                    )
                    if (uri != null) {
                        // Necessary because somehow the MediaStoreObserver doesn't work for playlists
                        context.contentResolver.notifyChange(uri, null)
                        Toast.makeText(context, context.resources.getString(R.string.created_playlist_x, name), Toast.LENGTH_SHORT).show()
                        id = uri.lastPathSegment!!.toLong()
                    }
                } else {
                    // Playlist exists
                    if (cursor.moveToFirst()) { id = cursor.getLong(0) }
                }
                cursor?.close()
            } catch (ignored: SecurityException) { }
        }
        if (id == -1L) {
            Toast.makeText(context, context.resources.getString(R.string.could_not_create_playlist), Toast.LENGTH_SHORT).show()
        }
        return id
    }

    fun renamePlaylist(context: Context, id: Long, newName: String) {
        val playlistUri = getPlaylistUris(context, id)
        try {
            context.contentResolver.update(playlistUri, ContentValues().also { it.put(PlaylistsColumns.NAME, newName) }, null, null)
            // Necessary because somehow the MediaStoreObserver doesn't work for playlists
            context.contentResolver.notifyChange(playlistUri, null)
        } catch (ignored: SecurityException) { }
    }

    fun addToPlaylist(context: Context, song: Song, playlistId: Long, showToastOnFinish: Boolean) =
        addToPlaylist(context, listOf(song), playlistId, showToastOnFinish)

    @Deprecated("")
    fun addToPlaylist(context: Context, songs: List<Song>, playlistId: Long, showToastOnFinish: Boolean) {

        val uri = getPlaylistUris(context, playlistId)
        var cursor: Cursor? = null
        var base = 0
        try {
            try {
                val projection = arrayOf("max(" + MediaStore.Audio.Playlists.Members.PLAY_ORDER + ")")
                cursor = context.contentResolver
                    .query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    base = cursor.getInt(0) + 1
                }
            } finally {
                cursor?.close()
            }

            var numInserted = 0
            var offSet = 0
            while (offSet < songs.size) {
                numInserted += context.contentResolver.bulkInsert(
                    uri, makeInsertItems(songs, offSet, 1000, base)
                )
                offSet += 1000
            }

            // Necessary because somehow the MediaStoreObserver doesn't work for playlists
            context.contentResolver.notifyChange(uri, null)
            if (showToastOnFinish) {
                Toast.makeText(
                    context,
                    context.resources.getString(R.string.inserted_x_songs_into_playlist_x, numInserted, getNameForPlaylist(context, playlistId)),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (ignored: SecurityException) { }
    }

    private fun makeInsertItems(songs: List<Song>, offset: Int, lenth: Int, base: Int): Array<ContentValues?> {
        var len = lenth
        if (offset + len > songs.size) {
            len = songs.size - offset
        }
        val contentValues = arrayOfNulls<ContentValues>(len)
        for (i in 0 until len) {
            contentValues[i] = ContentValues()
            contentValues[i]!!.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + offset + i)
            contentValues[i]!!.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songs[offset + i].id)
        }
        return contentValues
    }

    fun moveItem(context: Context, playlistId: Long, from: Int, to: Int): Boolean {
        val res = Playlists.Members.moveItem(context.contentResolver, playlistId, from, to)
        // Necessary because somehow the MediaStoreObserver doesn't work for playlists
        // NOTE: actually for now lets disable this because it messes with the animation (tested on Android 11)
//        context.contentResolver.notifyChange(getPlaylistUris(context, playlistId), null)
        return res
    }

    fun removeFromPlaylist(context: Context, song: Song, playlistId: Long) {
        val selection = Playlists.Members.AUDIO_ID + " =?"
        val selectionArgs = arrayOf(song.id.toString())
        try {
            context.contentResolver.delete(getPlaylistUris(context, playlistId), selection, selectionArgs)
            // Necessary because somehow the MediaStoreObserver doesn't work for playlists
            context.contentResolver.notifyChange(getPlaylistUris(context, playlistId), null)
        } catch (ignored: SecurityException) {
        }
    }

    fun removeFromPlaylist(context: Context, songs: List<PlaylistSong>) {
        val selectionArgs = arrayOfNulls<String>(songs.size)
        for (i in selectionArgs.indices) {
            selectionArgs[i] = songs[i].idInPlayList.toString()
        }

        var selection = Playlists.Members._ID + " IN ("
        for (selectionArg in selectionArgs) selection += "?, "
        selection = selection.substring(0, selection.length - 2) + ")"

        try {
            context.contentResolver.delete(getPlaylistUris(context, songs[0].playlistId), selection, selectionArgs)
            // Necessary because somehow the MediaStoreObserver is not notified when adding a playlist
            context.contentResolver.notifyChange(getPlaylistUris(context, songs[0].playlistId), null)
        } catch (ignored: SecurityException) {
        }
    }

    fun deletePlaylistsInDir(activity: Activity, playlists: List<Playlist>, treeUri: Uri) {

        GlobalScope.launch(Dispatchers.IO) {

            val documentDir = DocumentFile.fromTreeUri(activity, treeUri)
            val deleteList: MutableList<DocumentFile> = ArrayList()

            documentDir?.let { dir ->
                deleteList.addAll(
                    searchPlaylist(activity, dir, playlists)
                )
            }

            if (deleteList.isNotEmpty()) {
                val m = StringBuffer().append(Html.fromHtml(activity.resources.getQuantityString(R.plurals.msg_files_deletion_summary, deleteList.size, deleteList.size), Html.FROM_HTML_MODE_LEGACY))
                deleteList.forEach { file ->
                    Log.d("FILE_DEL", "DEL: ${file.name}@${file.uri}")
                    val name = file.uri.path.toString().substringAfter(":").substringAfter(":")
                    m.append(name).appendLine()
                }
                Log.v(TAG, deleteList.toString())
                withContext(Dispatchers.Main) {

//                    Looper.prepare()
                    val dialog = MaterialDialog(activity)
                        .title(R.string.delete_playlist_title)
                        .message(text = m)
                        .positiveButton(R.string.delete_action) {
                            deleteList.forEach { it.delete() }
                            Util.sentPlaylistChangedLocalBoardCast()
                        }
                        .negativeButton(android.R.string.cancel) { it.dismiss() }

                    dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(activity.getColor(R.color.md_red_A700))
                    dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(activity.getColor(R.color.md_green_A700))

                    dialog.show()

//                    Looper.loop()
                }
            } else {
                Util.coroutineToast(activity, R.string.failed_to_delete)
            }
        }
    }

    /**
     * WARNING: random order (perhaps)
     */
    fun getPlaylistFileNames(context: Context, playlists: List<Playlist>): List<String> {

        val ids: List<Long> = List(playlists.size) { playlists[it].id }

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            arrayOf(
                BaseColumns._ID /* 0 */,
                MediaStore.MediaColumns.DISPLAY_NAME /* 1 */
            ),
            null, null, null
        )

        val displayNames: MutableList<String> = ArrayList()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                ids.forEach { id ->
                    if (cursor.getLong(0) == id) {
                        displayNames.add(cursor.getString(1))
                    }
                }
            } while (cursor.moveToNext())
        }

        return if (displayNames.isNotEmpty()) displayNames else ArrayList()
    }

    fun getNameForPlaylist(context: Context, id: Long): String {
        try {
            val cursor = context.contentResolver.query(
                ContentUris.withAppendedId(Playlists.EXTERNAL_CONTENT_URI, id),
                arrayOf(PlaylistsColumns.NAME), null, null, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                cursor.use { return it.getString(0).orEmpty() }
            }
        } catch (ignored: SecurityException) { }
        return ""
    }

    fun getPlaylistUris(context: Context, playlists: Playlist): Uri =
        MediaStore.Audio.Playlists.Members.getContentUri("external", playlists.id)

    fun getPlaylistUris(context: Context, playlistsId: Long): Uri =
        MediaStore.Audio.Playlists.Members.getContentUri("external", playlistsId)

    fun doesPlaylistContain(context: Context, playlistId: Long, songId: Long): Boolean {
        if (playlistId != -1L) {
            try {
                val c = context.contentResolver.query(
                    getPlaylistUris(context, playlistId),
                    arrayOf(Playlists.Members.AUDIO_ID),
                    Playlists.Members.AUDIO_ID + "=?", arrayOf(songId.toString()), null
                )
                var count = 0
                if (c != null) {
                    count = c.count
                    c.close()
                }
                return count > 0
            } catch (ignored: SecurityException) {
            }
        }
        return false
    }

    fun searchPlaylist(context: Context, dir: DocumentFile, playlists: List<Playlist>): List<DocumentFile> {
        val fileNames = getPlaylistFileNames(context, playlists)
        if (fileNames.isEmpty()) {
            Log.w(TAG, "No playlist display name?")
            return ArrayList()
        }
        return searchFiles(context, dir, fileNames)
    }

    fun searchFiles(context: Context, dir: DocumentFile, filenames: List<String>): List<DocumentFile> {
        if (dir.isFile or dir.isVirtual) return ArrayList()
        val result: MutableList<DocumentFile> = ArrayList(1)
        if (dir.isDirectory) {
            dir.listFiles().forEach { sub ->
                if (sub.isFile) {
                    filenames.forEach { n ->
                        if (n == sub.name.orEmpty()) result.add(sub)
                    }
                }
                if (sub.isDirectory) {
                    result.addAll(
                        searchFiles(context, sub, filenames)
                    )
                }
            }
        }
        return result
    }
}

object PlaylistWriter {
    fun createPlaylistViaSAF(name: String, songs: List<Song>?, activity: SAFCallbackHandlerActivity) {
        val safLauncher: SafLauncher = activity.getSafLauncher()
        activity as ComponentActivity
        // prepare callback
        val uriCallback: UriCallback = { uri ->
            // callback start
            GlobalScope.launch(Dispatchers.IO) {
                if (uri == null) {
                    Util.coroutineToast(activity, R.string.failed)
                } else {
                    Util.sentPlaylistChangedLocalBoardCast()
                    try {
                        val outputStream = activity.contentResolver.openOutputStream(uri, "rw")
                        if (outputStream != null) {
                            try {
                                if (songs != null) M3UWriter.write(outputStream, songs)
                                Util.coroutineToast(activity, R.string.success)
                            } catch (e: IOException) {
                                Util.coroutineToast(
                                    activity,
                                    activity.getString(R.string.failed) + ":${uri.path} can not be written"
                                )
                            } finally {
                                outputStream.close()
                            }
                        }
                    } catch (e: FileNotFoundException) {
                        Util.coroutineToast(
                            activity,
                            activity.getString(R.string.failed) + ":${uri.path} is not available"
                        )
                    }
                }
            }
            // callback end
        }

        GlobalScope.launch(Dispatchers.IO) {
            while (safLauncher.createCallbackInUse) yield()
            try {
                safLauncher.createFile("$name.m3u", uriCallback)
            } catch (e: Exception) {
                Util.coroutineToast(activity, activity.getString(R.string.failed) + ": unknown")
                Log.i("CreatePlaylistDialog", "SaveFail: \n${e.message}")
            }
        }
    }

    fun createPlaylist(name: String, songs: List<Song>?, context: Context) {
        val playlistId = PlaylistsUtil.createPlaylist(context, name)
        if (songs != null && songs.isNotEmpty()) {
            PlaylistsUtil.addToPlaylist(context, songs, playlistId, true)
        }
    }
}

object M3UWriter {
    private const val EXTENSION = "m3u"
    private const val HEADER = "#EXTM3U"
    private const val ENTRY = "#EXTINF:"
    private const val DURATION_SEPARATOR = ","

    private fun createLine(song: Song): String {
        val b = StringBuffer()
        b.appendLine()
        b.append("$ENTRY${song.duration}$DURATION_SEPARATOR${song.artistName} - ${song.title}")
        b.appendLine()
        b.append(song.data)
        return b.toString()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun write(context: Context?, dir: File, playlist: Playlist): File {
        if (!dir.exists()) dir.mkdirs() //noinspection ResultOfMethodCallIgnored
        val filename: String

        val songs: List<Song>
        if (playlist is AbsCustomPlaylist) {
            songs = playlist.getSongs(context)

            // Since AbsCustomPlaylists are dynamic, we add a timestamp after their names.
            filename =
                playlist.name + SimpleDateFormat("_yy-MM-dd_HH-mm", Locale.getDefault()).format(
                Calendar.getInstance().time
            )
        } else {
            songs = PlaylistSongLoader.getPlaylistSongList(context!!, playlist.id)
            filename = playlist.name
        }

        val file = File(dir, "$filename.$EXTENSION")
        if (songs.isNotEmpty()) {
            val bw = BufferedWriter(FileWriter(file))

            bw.write(HEADER)
            for (song in songs) {
                bw.write(
                    createLine(song)
                )
            }

            bw.close()
        }

        return file
    }

    @JvmStatic
    @Throws(IOException::class)
    fun write(outputStream: OutputStream, context: Context?, playlist: Playlist) {

        val songs: List<Song> =
            if (playlist is AbsCustomPlaylist) {
                playlist.getSongs(context)
            } else {
                PlaylistSongLoader.getPlaylistSongList(context ?: App.instance, playlist.id)
            }

        if (songs.isNotEmpty()) {
            write(outputStream, songs)
        }
        return
    }

    @JvmStatic
    @Throws(IOException::class)
    fun write(outputStream: OutputStream, songs: List<Song>) {
        if (songs.isNotEmpty()) {
            val bw = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))

            bw.write(HEADER)
            for (song in songs) {
                bw.write(
                    createLine(song)
                )
            }

            bw.close()
        }
        return
    }
    @JvmStatic
    @Throws(IOException::class)
    fun append(outputStream: OutputStream, songs: List<Song>) {
        if (songs.isNotEmpty()) {
            val bw = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))

            for (song in songs) {
                bw.write(
                    createLine(song)
                )
            }
            bw.close()
        }
    }
}
