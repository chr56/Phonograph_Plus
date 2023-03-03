/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package util.phonograph.m3u

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import legacy.phonograph.MediaStoreCompat
import player.phonograph.App
import player.phonograph.R
import lib.phonograph.misc.CreateFileStorageAccessTool
import lib.phonograph.misc.OpenDirStorageAccessTool
import lib.phonograph.misc.OpenDocumentContract
import lib.phonograph.misc.OpenFileStorageAccessTool
import lib.phonograph.storage.getAbsolutePath
import lib.phonograph.uri.guessDocumentUri
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.notification.ErrorNotification
import player.phonograph.util.CoroutineUtil.coroutineToast
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.StringUtil
import player.phonograph.util.StringUtil.buildDeletionMessage
import player.phonograph.util.Util.reportError
import player.phonograph.util.Util.sentPlaylistChangedLocalBoardCast
import util.phonograph.m3u.internal.M3UGenerator
import util.phonograph.m3u.internal.appendTimestampSuffix
import androidx.annotation.StringRes
import androidx.core.provider.DocumentsContractCompat
import androidx.documentfile.provider.DocumentFile
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

object FileOperator {

    private inline fun execute(
        block: () -> Unit,
        message: String,
        tag: String = "FileOperator",
        extraMessage: String? = null,
        //exception: Class<Exception> = Exception::class.java
    ) {
        try {
            block()
        } catch (e: Exception) {
            //if (e.javaClass == exception) {
            reportError(e, tag, "Failed! $message $extraMessage")
            safeToast(App.instance, "Failed! $message")
            //}
        }
    }

    fun createPlaylistViaSAF(
        context: Context,
        name: String,
        songs: List<Song>?,
        accessTool: CreateFileStorageAccessTool
    ) {
        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            while (accessTool.busy) yield()
            execute(
                {
                    accessTool.launch("$name.m3u") { uri ->
                        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                            if (uri == null) {
                                safeToast(context, R.string.failed)
                            } else {
                                sentPlaylistChangedLocalBoardCast()
                                try {
                                    context.contentResolver.openOutputStream(uri, "rw")
                                        ?.use { stream ->
                                            try {
                                                if (songs != null) M3UGenerator.generate(
                                                    stream,
                                                    songs,
                                                    true
                                                )
                                                safeToast(context, R.string.success)
                                            } catch (e: IOException) {
                                                val message = "${uri.path} can not be written"
                                                safeToast(
                                                    context,
                                                    "${context.getString(R.string.failed)}: $message"
                                                )
                                                reportError(e, "CreatePlaylist",
                                                            "$message\nSongs:${
                                                                songs?.map { it.data }
                                                                    ?.reduce { acc, s -> "$acc,$s" }
                                                            }\nActivity:$context"
                                                )
                                            }
                                        }
                                } catch (e: FileNotFoundException) {
                                    val message = "${uri.path} is not available"
                                    safeToast(
                                        context,
                                        "${context.getString(R.string.failed)}:$message"
                                    )
                                    ErrorNotification.postErrorNotification(
                                        e,
                                        "$message\nSongs:${
                                            songs?.map { it.data }?.reduce { acc, s -> "$acc,$s" }
                                        }\nActivity:$context"
                                    )
                                }
                            }
                        }
                    }
                },
                "Failed to save!", "CreatePlaylist"
            )
        }
    }

    fun appendToPlaylistViaSAF(
        context: Context,
        songs: List<Song>,
        playlistId: Long,
        removeDuplicated: Boolean,
        accessTool: OpenFileStorageAccessTool
    ) {
        if (songs.isEmpty()) return
        val playlist = PlaylistsUtil.getPlaylist(context, playlistId)
        appendToPlaylistViaSAF(context, songs, playlist, removeDuplicated, accessTool)
    }

    fun appendToPlaylistViaSAF(
        context: Context,
        songs: List<Song>,
        filePlaylist: FilePlaylist,
        removeDuplicated: Boolean,
        accessTool: OpenFileStorageAccessTool
    ) {
        if (songs.isEmpty()) return

        val playlistPath = PlaylistsUtil.getPlaylistPath(context, filePlaylist)
        val documentUri = guessDocumentUri(context,File(playlistPath))

        val cfg = OpenDocumentContract.Config(
            arrayOf(
                "audio/x-mpegurl",
                MediaStoreCompat.Audio.Playlists.CONTENT_TYPE,
                MediaStoreCompat.Audio.Playlists.ENTRY_CONTENT_TYPE
            ),
            documentUri,
            false
        )
        accessTool.launch(cfg) { uri: Uri? ->
            if (uri != null) {
                CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                    try {
                        if (!assertUri(context, filePlaylist, uri)) {
                            val returningPath =
                                DocumentFile.fromSingleUri(context, uri)?.getAbsolutePath(context)!!
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
                            context.getString(
                                R.string.failed_to_save_playlist,
                                filePlaylist.name
                            ) + ": ${uri.path} is not available"
                        )
                    } catch (e: IOException) {
                        coroutineToast(
                            context,
                            context.getString(
                                R.string.failed_to_save_playlist,
                                filePlaylist.name
                            ) + ": Unknown!"
                        )
                    }
                }
            }
        }
    }

    fun assertUri(context: Context, target: FilePlaylist, uri: Uri): Boolean {
        val playlistPath = PlaylistsUtil.getPlaylistPath(context, target)
        val documentFile = DocumentFile.fromSingleUri(context, uri) ?: return false
        return documentFile.getAbsolutePath(context)!! == playlistPath
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
                    return@async PlaylistsUtil.searchPlaylist(activity, folder, filePlaylists)
                        .toMutableList()
                }

                val prepareList: MutableList<DocumentFile> = playlistInFolder.await()
                val deleteList: MutableList<DocumentFile> = ArrayList(prepareList.size / 2)

                if (prepareList.isEmpty()) {
                    // no playlist found in folder?
                    coroutineToast(activity, R.string.failed_to_delete)
                } else {
                    val playlistPaths = mediaStorePaths.await()
                    // valid playlists
                    for (file in prepareList) {
                        val filePath = file.getAbsolutePath(activity) ?: continue
                        if (filePath.endsWith("m3u", ignoreCase = true) or
                            filePath.endsWith("m3u8",ignoreCase = true)
                        ) {
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
                            deleteList.mapNotNull { file ->
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

    fun createPlaylistsViaSAF(
        context: Context,
        playlists: List<Playlist>,
        accessTool: OpenDirStorageAccessTool
    ) {
        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            while (accessTool.busy) yield()
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
                                            val outputStream =
                                                context.contentResolver.openOutputStream(
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
                accessTool.launch(parent.uri) { uriCallback(it) }
            } catch (e: Exception) {
                coroutineToast(context, context.getString(R.string.failed) + ": unknown")
                ErrorNotification.postErrorNotification(e, "unknown")
                Log.w("CreatePlaylistDialog", "SaveFail: \n${e.message}")
            }
        }
    }


    private fun safeToast(context: Context, message: String) {
        val ready = Looper.myLooper() != null
        if (!ready) Looper.prepare()
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        if (!ready) Looper.loop()
    }

    private fun safeToast(context: Context, @StringRes id: Int) {
        val ready = Looper.myLooper() != null
        if (!ready) Looper.prepare()
        Toast.makeText(context, id, Toast.LENGTH_SHORT).show()
        if (!ready) Looper.loop()
    }
}


typealias UriCallback = (Uri?) -> Any