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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.provider.DocumentsContractCompat
import androidx.documentfile.provider.DocumentFile
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.*
import lib.phonograph.storage.getAbsolutePath
import player.phonograph.R
import player.phonograph.misc.OpenDocumentContract
import player.phonograph.misc.SafLauncher
import player.phonograph.misc.UriCallback
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.notification.ErrorNotification
import player.phonograph.util.CoroutineUtil.coroutineToast
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.StringUtil
import player.phonograph.util.StringUtil.buildDeletionMessage
import player.phonograph.util.Util.sentPlaylistChangedLocalBoardCast
import util.phonograph.m3u.internal.M3UGenerator
import util.phonograph.m3u.internal.appendTimestampSuffix

object FileOperator {

    fun createPlaylistViaSAF(
        name: String,
        songs: List<Song>?,
        safLauncher: SafLauncher,
        activity: ComponentActivity
    ) {
        // prepare callback
        val uriCallback: UriCallback = { uri ->
            // callback start
            CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
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
                                    activity,
                                    activity.getString(R.string.failed) + ":${uri.path} can not be written",
                                    true
                                )
                                ErrorNotification.postErrorNotification(
                                    e,
                                    "${uri.path} can not be written.\nSongs:${
                                    songs?.map { it.data }?.reduce { acc, s -> "$acc,$s" }
                                    }\nActivity:$activity"
                                )
                            } finally {
                                outputStream.close()
                            }
                        }
                    } catch (e: FileNotFoundException) {
                        coroutineToast(
                            activity,
                            activity.getString(R.string.failed) + ":${uri.path} is not available"
                        )
                        ErrorNotification.postErrorNotification(
                            e,
                            "${uri.path} is not available.\nSongs:${
                            songs?.map { it.data }?.reduce { acc, s -> "$acc,$s" }
                            }\nActivity:$activity"
                        )
                    }
                }
            }
            // callback end
        }

        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            while (safLauncher.createCallbackInUse) yield()
            try {
                safLauncher.createFile("$name.m3u", uriCallback)
            } catch (e: Exception) {
                coroutineToast(activity, activity.getString(R.string.failed) + ": unknown")
                ErrorNotification.postErrorNotification(e, "Unknown")
                Log.w("CreatePlaylistDialog", "SaveFail: \n${e.message}")
            }
        }
    }

    fun appendToPlaylistViaSAF(
        songs: List<Song>,
        playlistId: Long,
        removeDuplicated: Boolean,
        context: Context,
        safLauncher: SafLauncher
    ) {
        if (songs.isEmpty()) return
        val playlist = PlaylistsUtil.getPlaylist(context, playlistId)
        appendToPlaylistViaSAF(songs, playlist, removeDuplicated, context, safLauncher)
    }

    fun appendToPlaylistViaSAF(
        songs: List<Song>,
        filePlaylist: FilePlaylist,
        removeDuplicated: Boolean,
        context: Context,
        safLauncher: SafLauncher
    ) {
        if (songs.isEmpty()) return

        val playlistPath = PlaylistsUtil.getPlaylistPath(context, filePlaylist)
        val playlistDocumentFile =
            DocumentFile.fromFile(File(playlistPath)).parentFile ?: DocumentFile.fromFile(
                Environment.getExternalStorageDirectory()
            )

        val cfg = OpenDocumentContract.Cfg(
            playlistDocumentFile.uri,
            arrayOf(
                "audio/x-mpegurl",
                MediaStore.Audio.Playlists.CONTENT_TYPE,
                MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE
            ),
            false
        )
        safLauncher.openFile(cfg) { uri: Uri? ->
            if (uri != null) {
                CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                    try {
                        if (!assertUri(context, filePlaylist, uri)) {
                            val returningPath = DocumentFile.fromSingleUri(context, uri)?.getAbsolutePath(
                                context
                            )
                            val errorMsg =
                                "${
                                context.getString(
                                    R.string.failed_to_save_playlist,
                                    filePlaylist.name
                                )
                                }: ${context.getString(R.string.file_incorrect)}" +
                                    "Playlist($playlistPath) -> File($returningPath) "
                            Log.e("AppendToPlaylist", errorMsg)
                            coroutineToast(context, errorMsg, true)
                            ErrorNotification.postErrorNotification(
                                IllegalStateException(
                                    "Write for Playlist($playlistPath) but we got File($returningPath) "
                                ),
                                "SAF uri: $uri, Playlist:$playlistPath"
                            )
                            return@launch
                        }

                        val outputStream = context.contentResolver.openOutputStream(uri, "wa")
                        outputStream?.use {
                            M3UGenerator.generate(outputStream, songs, false)
                            coroutineToast(context, context.getString(R.string.success))
                        }
                    } catch (e: FileNotFoundException) {
                        coroutineToast(
                            context,
                            context.getString(R.string.failed_to_save_playlist, filePlaylist.name) + ": ${uri.path} is not available"
                        )
                    } catch (e: IOException) {
                        coroutineToast(
                            context,
                            context.getString(R.string.failed_to_save_playlist, filePlaylist.name) + ": Unknown!"
                        )
                    }
                }
            }
        }
    }

    fun assertUri(context: Context, target: FilePlaylist, uri: Uri): Boolean {
        val playlistPath = PlaylistsUtil.getPlaylistPath(context, target)
        val documentFile = DocumentFile.fromSingleUri(context, uri) ?: return false
        return documentFile.getAbsolutePath(context) == playlistPath
    }

    fun deletePlaylistsViaSAF(activity: Activity, filePlaylists: List<FilePlaylist>, treeUri: Uri) {
        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            val folder =
                if (DocumentsContractCompat.isTreeUri(treeUri)) {
                    DocumentFile.fromTreeUri(activity, treeUri)
                } else null

            if (folder != null) {
                val coroutineScope = CoroutineScope(Dispatchers.IO)

                // get given playlist paths
                val mediaStorePaths = coroutineScope.async {
                    return@async filePlaylists.map { PlaylistsUtil.getPlaylistPath(activity, it) }
                }
                // search playlist in folder
                val playlistInFolder = coroutineScope.async {
                    return@async PlaylistsUtil.searchPlaylist(activity, folder, filePlaylists).toMutableList()
                }

                val prepareList: MutableList<DocumentFile> = playlistInFolder.await()
                val deleteList: MutableList<DocumentFile> = ArrayList(prepareList.size / 2)

                if (prepareList.isEmpty()) {
                    // no playlist found in folder?
                    coroutineToast(activity, R.string.failed_to_delete)
                } else {
                    val playlistPaths = mediaStorePaths.await()
                    // valid playlists
                    prepareList.forEach { file ->
                        val filePath = file.getAbsolutePath(activity)
                        if (filePath.endsWith("m3u", ignoreCase = true) or filePath.endsWith(
                                "m3u8",
                                ignoreCase = true
                            )) {
                            for (p in playlistPaths) {
                                if (p == filePath) deleteList.add(file)
                            }
                        }
                    }

                    // confirm to delete
                    val message = buildDeletionMessage(
                        context = activity,
                        itemSize = deleteList.size,
                        null,
                        StringUtil.ItemGroup(
                            activity.resources
                                .getQuantityString(R.plurals.item_files, deleteList.size),
                            deleteList.map { file ->
                                Log.v("FileDelete", "${file.name}@${file.uri}")
                                file.getAbsolutePath(activity)
                            }
                        )
                    )

                    withContext(Dispatchers.Main) {
                        MaterialDialog(activity)
                            .title(R.string.delete_action)
                            .message(text = message)
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
                ErrorNotification.postErrorNotification(
                    IllegalStateException("$treeUri is invalid"),
                    "Select correct folder!"
                )
            }
        }
    }

    fun createPlaylistsViaSAF(playlists: List<Playlist>, context: Context, safLauncher: SafLauncher) {
        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            while (safLauncher.openCallbackInUse) yield()
            try {
                // callback
                val uriCallback: UriCallback = { treeUri ->
                    CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                        if (treeUri == null) {
                            coroutineToast(context, R.string.failed)
                        } else {
                            try {
                                val dir = DocumentFile.fromTreeUri(context, treeUri)
                                if (dir != null && dir.isDirectory) {
                                    playlists.forEach { playlist ->
                                        val file = dir.createFile(
                                            "audio/x-mpegurl",
                                            appendTimestampSuffix(playlist.name)
                                        )
                                        if (file != null) {
                                            val outputStream = context.contentResolver.openOutputStream(
                                                file.uri
                                            )
                                            if (outputStream != null) {
                                                val songs: List<Song> = playlist.getSongs(context)
                                                M3UGenerator.generate(outputStream, songs, true)
                                            } else {
                                                coroutineToast(
                                                    context,
                                                    context.getString(
                                                        R.string.failed_to_save_playlist,
                                                        playlist.name
                                                    )
                                                )
                                            }
                                        } else {
                                            coroutineToast(
                                                context,
                                                context.getString(
                                                    R.string.failed_to_save_playlist,
                                                    playlist.name
                                                )
                                            )
                                        }
                                    }
                                    sentPlaylistChangedLocalBoardCast()
                                }
                            } catch (e: FileNotFoundException) {
                                coroutineToast(
                                    context,
                                    context.getString(R.string.failed) + ":${treeUri.path} is not available"
                                )
                                ErrorNotification.postErrorNotification(
                                    e,
                                    "${treeUri.path} is not available"
                                )
                            }
                        }
                    }
                }

                val parent = DocumentFile.fromFile(
                    File(PlaylistsUtil.getPlaylistPath(context, playlists[0] as FilePlaylist))
                ).parentFile
                    ?: DocumentFile.fromFile(Environment.getExternalStorageDirectory())

                coroutineToast(
                    context,
                    context.getString(R.string.direction_open_folder_with_saf),
                    true
                )
                safLauncher.openDir(parent.uri, uriCallback)
            } catch (e: Exception) {
                coroutineToast(context, context.getString(R.string.failed) + ": unknown")
                ErrorNotification.postErrorNotification(e, "unknown")
                Log.w("CreatePlaylistDialog", "SaveFail: \n${e.message}")
            }
        }
    }
}
