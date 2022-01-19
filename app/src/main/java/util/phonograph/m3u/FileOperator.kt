/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package util.phonograph.m3u

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.Html
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.documentfile.provider.DocumentFile
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import kotlinx.coroutines.*
import player.phonograph.R
import player.phonograph.model.Playlist
import player.phonograph.model.Song
import player.phonograph.util.OpenDocumentContract
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.SafLauncher
import player.phonograph.util.UriCallback
import player.phonograph.util.Util.coroutineToast
import player.phonograph.util.Util.sentPlaylistChangedLocalBoardCast
import util.phonograph.m3u.internal.M3UGenerator
import java.io.FileNotFoundException
import java.io.IOException

object FileOperator {

    fun createPlaylistViaSAF(name: String, songs: List<Song>?, safLauncher: SafLauncher, activity: ComponentActivity) {

        // prepare callback
        val uriCallback: UriCallback = { uri ->
            // callback start
            GlobalScope.launch(Dispatchers.IO) {
                if (uri == null) {
                    coroutineToast(activity, R.string.failed)
                } else {
                    sentPlaylistChangedLocalBoardCast()
                    try {
                        val outputStream = activity.contentResolver.openOutputStream(uri, "rw")
                        if (outputStream != null) {
                            try {
                                if (songs != null) M3UGenerator.generate(outputStream, songs, true)
                                coroutineToast(activity, R.string.success)
                            } catch (e: IOException) {
                                coroutineToast(
                                    activity, activity.getString(R.string.failed) + ":${uri.path} can not be written", true
                                )
                            } finally {
                                outputStream.close()
                            }
                        }
                    } catch (e: FileNotFoundException) {
                        coroutineToast(
                            activity, activity.getString(R.string.failed) + ":${uri.path} is not available"
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
                coroutineToast(activity, activity.getString(R.string.failed) + ": unknown")
                Log.i("CreatePlaylistDialog", "SaveFail: \n${e.message}")
            }
        }
    }

    fun appendToPlaylistViaSAF(songs: List<Song>, playlistId: Long, removeDuplicated: Boolean, context: Context, safLauncher: SafLauncher) {
        if (songs.isEmpty()) return
        val playlist = PlaylistsUtil.getPlaylist(context, playlistId)
        appendToPlaylistViaSAF(songs, playlist, removeDuplicated, context, safLauncher)
    }

    // todo remove hardcode
    fun appendToPlaylistViaSAF(songs: List<Song>, playlist: Playlist, removeDuplicated: Boolean, context: Context, safLauncher: SafLauncher) {
        if (songs.isEmpty()) return

        val rawPath = PlaylistsUtil.getPlaylistPath(context, playlist)

        val regex = "/(sdcard)|(storage/emulated)/\\d+/".toRegex()
        val path = regex.replace(rawPath.removePrefix(Environment.getExternalStorageDirectory().absolutePath), "")

        @Suppress("SpellCheckingInspection")
        val parentFolderUri = Uri.parse(
            "content://com.android.externalstorage.documents/document/primary:" + Uri.encode(path)
        )

        val cfg = OpenDocumentContract.Cfg(parentFolderUri, arrayOf("audio/x-mpegurl", MediaStore.Audio.Playlists.CONTENT_TYPE, MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE), false)
        safLauncher.openFile(cfg) { uri: Uri? ->
            if (uri != null) {
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        // valid uri
                        val parsedUriPath = uri.path!!.substringAfter("/document/").substringAfter(":").substringAfter(":")
                        if (!parsedUriPath.contains(path.substringAfter('/'))) {
                            val errorMsg = "${ context.getString(R.string.failed_to_save_playlist, playlist.name) }: ${context.getString(R.string.file_incorrect)}" +
                                " $path -> $parsedUriPath "
                            Log.e("appendToPlaylist", errorMsg)
                            coroutineToast(context, errorMsg, true)
                            return@launch
                        }

                        val outputStream = context.contentResolver.openOutputStream(uri, "wa")
                        outputStream?.let {
                            M3UGenerator.generate(outputStream, songs, false)
                            coroutineToast(context, context.getString(R.string.success))
                        }
                    } catch (e: FileNotFoundException) {
                        coroutineToast(context, context.getString(R.string.failed_to_save_playlist, playlist.name) + ": ${uri.path} is not available")
                    } catch (e: IOException) {
                        coroutineToast(context, context.getString(R.string.failed_to_save_playlist, playlist.name) + ": Unknown!")
                    }
                }
            }
        }
    }

    fun deletePlaylistsViaSAF(activity: Activity, playlists: List<Playlist>, treeUri: Uri) {

        GlobalScope.launch(Dispatchers.IO) {
            val folder = DocumentFile.fromTreeUri(activity, treeUri)
            if (folder != null) {
                val coroutineScope = CoroutineScope(Dispatchers.IO)

                // get given playlist paths
                val mediaStorePaths = coroutineScope.async {
                    val paths: MutableList<String> = ArrayList(playlists.size)
                    playlists.forEach {
                        paths.add(PlaylistsUtil.getPlaylistPath(activity, it))
                    }
                    return@async paths
                }
                // search playlist in folder
                val playlistInFolder = coroutineScope.async {
                    val deleteList: MutableList<DocumentFile> = ArrayList()
                    deleteList.addAll(
                        PlaylistsUtil.searchPlaylist(activity, folder, playlists)
                    )
                    return@async deleteList
                }

                val prepareList: MutableList<DocumentFile> = playlistInFolder.await()
                val deleteList: MutableList<DocumentFile> = ArrayList(prepareList.size / 2)

                if (prepareList.isNullOrEmpty()) {
                    // no playlist found in folder?
                    coroutineToast(activity, R.string.failed_to_delete)
                } else {
                    val playlistPaths = mediaStorePaths.await()
                    // valid playlists
                    prepareList.forEach { file ->
                        // todo remove hardcode
                        val fileUriPath = file.uri.path!!.substringAfter("/document/")
                        if (fileUriPath.endsWith("m3u", ignoreCase = true) or fileUriPath.endsWith("m3u8", ignoreCase = true)) {
                            val path = fileUriPath.substringAfter(":").substringAfter(":")
                            playlistPaths.forEach {
                                if (it.contains(path, ignoreCase = true)) deleteList.add(file)
                            }
                        }
                    }

                    // confirm to delete
                    val m = StringBuffer().append(Html.fromHtml(activity.resources.getQuantityString(R.plurals.msg_files_deletion_summary, deleteList.size, deleteList.size), Html.FROM_HTML_MODE_LEGACY))
                    deleteList.forEach { file ->
                        m.append(file.uri.path!!.substringAfter("/document/").substringAfter(":").substringAfter(":")).appendLine()
                        Log.i("FileDelete", "DeleteList:")
                        Log.i("FileDelete", "${file.name}@${file.uri}")
                    }

                    withContext(Dispatchers.Main) {

                        MaterialDialog(activity)
                            .title(R.string.delete_playlist_title)
                            .message(text = m)
                            .positiveButton(R.string.delete_action) {
                                prepareList.forEach { it.delete() }
                                sentPlaylistChangedLocalBoardCast()
                            }
                            .negativeButton(android.R.string.cancel) { it.dismiss() }
                            .also {
                                it.getActionButton(WhichButton.POSITIVE)
                                    .updateTextColor(activity.getColor(R.color.md_red_A700))
                                it.getActionButton(WhichButton.NEGATIVE)
                                    .updateTextColor(activity.getColor(R.color.md_green_A700))
                            }
                            .show()
                    }
                }
            } else {
                // folder unavailable
                coroutineToast(activity, R.string.failed_to_delete)
            }
        }
    }
}
