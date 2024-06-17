/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist.saf

import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.SAFActivityResultContracts.chooseDirViaSAF
import lib.storage.launcher.SAFActivityResultContracts.createFileViaSAF
import player.phonograph.R
import player.phonograph.mechanism.playlist.m3u.M3UWriter
import player.phonograph.model.Song
import player.phonograph.util.PLAYLIST_MIME_TYPE
import player.phonograph.util.coroutineToast
import player.phonograph.util.openOutputStreamSafe
import player.phonograph.util.reportError
import player.phonograph.util.sentPlaylistChangedLocalBoardCast
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffix
import player.phonograph.util.warning
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.IOException

/**
 * @param context must be [IOpenFileStorageAccessible]
 */
suspend fun createPlaylistViaSAF(
    context: Context,
    playlistName: String,
    songs: List<Song>,
): Unit = withContext(Dispatchers.IO) {
    // check
    require(context is IOpenFileStorageAccessible)
    while (context.openFileStorageAccessDelegate.busy) yield()
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
 * @param context must be [IOpenFileStorageAccessible]
 */
suspend fun createPlaylistsViaSAF(
    context: Context,
    songBatches: List<List<Song>>,
    names: List<String>,
    location: String,
) = withContext(Dispatchers.IO) {
    // check
    if (songBatches.isEmpty()) return@withContext
    require(context is IOpenFileStorageAccessible)
    while (context.openFileStorageAccessDelegate.busy) yield()
    // launch
    val treeUri = chooseDirViaSAF(context, location)

    val parentDocumentUri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))

    // execute
    val failedToCreate: MutableList<String> = mutableListOf()
    val failedToWrite: MutableList<String> = mutableListOf()

    for ((index, songBatch) in songBatches.withIndex()) {
        val childUri: Uri? =
            try {
                DocumentsContract.createDocument(
                    context.contentResolver,
                    parentDocumentUri,
                    PLAYLIST_MIME_TYPE,
                    "${names[index]}${dateTimeSuffix(currentDate())}"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                failedToCreate.add(names[index])
                null
            }
        if (childUri != null) {
            openOutputStreamSafe(context, childUri, "rwt")?.use { outputStream ->
                try {
                    M3UWriter.write(outputStream, songBatch, true)
                } catch (e: IOException) {
                    failedToWrite.add(names[index])
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
            for (playlistName in failedToCreate) {
                append("Failed to create playlist ${playlistName}\n")
            }
            for (playlistName in failedToWrite) {
                append("Failed to write playlist ${playlistName}\n")
            }
        }
        warning(TAG, message)
    } else {
        coroutineToast(context, R.string.success)
    }

}


private const val TAG = "PlaylistCreate"
