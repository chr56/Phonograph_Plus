/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.saf

import lib.activityresultcontract.ActivityResultContractUtil.chooseDirViaSAF
import lib.activityresultcontract.ActivityResultContractUtil.createFileViaSAF
import lib.activityresultcontract.IOpenFileStorageAccess
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.util.PLAYLIST_MIME_TYPE
import player.phonograph.util.coroutineToast
import player.phonograph.util.openOutputStreamSafe
import player.phonograph.util.reportError
import player.phonograph.util.sentPlaylistChangedLocalBoardCast
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffix
import player.phonograph.util.warning
import util.phonograph.playlist.m3u.M3UWriter
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.IOException

/**
 * @param context must be [IOpenFileStorageAccess]
 */
suspend fun createPlaylistViaSAF(
    context: Context,
    playlistName: String,
    songs: List<Song>,
): Unit = withContext(Dispatchers.IO) {
    // check
    require(context is IOpenFileStorageAccess)
    while (context.openFileStorageAccessTool.busy) yield()
    // launch
    val uri = createFileViaSAF(context, "$playlistName.m3u")
    openOutputStreamSafe(context, uri, "rwt")?.use { stream ->
        try {
            M3UWriter.write(stream, songs, true)
            coroutineToast(context, R.string.success)
            delay(250)
            sentPlaylistChangedLocalBoardCast()
        } catch (e: Exception) {
            reportError(e, TAG, "Failed to write $uri")
            coroutineToast(context, R.string.failed)
        }
    }
}


/**
 * @param context must be [IOpenFileStorageAccess]
 */
suspend fun createPlaylistsViaSAF(
    context: Context,
    playlists: List<Playlist>,
    initialPosition: String,
) = withContext(Dispatchers.IO) {
    // check
    if (playlists.isEmpty()) return@withContext
    require(context is IOpenFileStorageAccess)
    while (context.openFileStorageAccessTool.busy) yield()
    // launch
    val treeUri = chooseDirViaSAF(context, initialPosition)

    val parentDocumentUri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))

    val failedToCreate: MutableList<Playlist> = mutableListOf()
    val failedToWrite: MutableList<Playlist> = mutableListOf()

    for (playlist in playlists) {
        val childUri: Uri? =
            try {
                DocumentsContract.createDocument(
                    context.contentResolver,
                    parentDocumentUri,
                    PLAYLIST_MIME_TYPE,
                    "${playlist.name}${dateTimeSuffix(currentDate())}"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                failedToCreate.add(playlist)
                null
            }
        if (childUri != null) {
            openOutputStreamSafe(context, childUri, "rwt")?.use { outputStream ->
                val songs: List<Song> = playlist.getSongs(context)
                try {
                    M3UWriter.write(outputStream, songs, true)
                } catch (e: IOException) {
                    failedToWrite.add(playlist)
                    e.printStackTrace()
                }
            }
        }
    }

    if (failedToCreate.isNotEmpty() || failedToWrite.isNotEmpty()) {
        coroutineToast(context, R.string.failed)
        val message = buildString {
            append("Tree     Uri: ${treeUri.path} \n")
            append("Document Uri: ${parentDocumentUri.path} \n")
            for (playlist in failedToCreate) {
                append("Failed to create playlist ${playlist.name}\n")
            }
            for (playlist in failedToWrite) {
                append("Failed to write playlist ${playlist.name}\n")
            }
        }
        warning(TAG, message)
    } else {
        coroutineToast(context, R.string.success)
    }
}


private const val TAG = "PlaylistCreate"
