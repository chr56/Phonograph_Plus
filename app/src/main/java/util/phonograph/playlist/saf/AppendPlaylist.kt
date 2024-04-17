/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.saf

import lib.activityresultcontract.ActivityResultContractUtil.chooseDirViaSAF
import lib.activityresultcontract.IOpenFileStorageAccess
import lib.storage.getBasePath
import lib.storage.getStorageId
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.util.coroutineToast
import player.phonograph.util.warning
import util.phonograph.playlist.m3u.M3UWriter
import android.content.Context
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.IOException

/**
 * @param context must be [IOpenFileStorageAccess]
 */
suspend fun appendToPlaylistViaSAF(
    context: Context,
    songs: List<Song>,
    filePlaylist: FilePlaylist,
) = withContext(Dispatchers.IO) {
    //
    // check
    //
    if (songs.isEmpty()) return@withContext
    require(context is IOpenFileStorageAccess)
    require(filePlaylist.id > 0 || filePlaylist.associatedFilePath.contains('/'))
    while (context.openFileStorageAccessTool.busy) yield()
    //
    // select
    //
    val playlistPath = filePlaylist.associatedFilePath
    val treeUri = chooseDirViaSAF(context, playlistPath)
    val documentId = run {
        val file = File(playlistPath)
        val storageId = file.getStorageId(context)
        val basePath = file.getBasePath()
        "$storageId:$basePath"
    }
    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

    //
    // write
    //
    try {
        openOutputStreamSafe(context, childUri, "wa")?.use { outputStream ->
            M3UWriter.write(outputStream, songs, false)
            coroutineToast(context, context.getString(R.string.success))
        }
    } catch (e: IOException) {
        warning(TAG, "Failed write child uri: $childUri (from $playlistPath)")
        coroutineToast(context, context.getString(R.string.failed_to_save_playlist, filePlaylist.name))
    }
}

private const val TAG = "PlaylistAppend"