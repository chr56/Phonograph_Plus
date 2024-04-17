/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.saf

import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import lib.activityresultcontract.ActivityResultContractUtil
import lib.activityresultcontract.ActivityResultContractUtil.chooseDirViaSAF
import lib.activityresultcontract.ActivityResultContractUtil.chooseFileViaSAF
import lib.activityresultcontract.IOpenFileStorageAccess
import lib.storage.externalFileBashPath
import lib.storage.getAbsolutePath
import lib.storage.getBasePath
import lib.storage.getStorageId
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.util.coroutineToast
import player.phonograph.util.debug
import player.phonograph.util.warning
import util.phonograph.playlist.m3u.M3UWriter
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
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

    val uri = getContentUri(context, playlistPath)
    if (uri == null) {
        coroutineToast(context, context.getString(R.string.failed))
        return@withContext
    }
    //
    // write
    //
    try {
        openOutputStreamSafe(context, uri, "wa")?.use { outputStream ->
            M3UWriter.write(outputStream, songs, false)
            coroutineToast(context, context.getString(R.string.success))
        }
    } catch (e: IOException) {
        warning(TAG, "Failed write playlist via uri: $uri (from file $playlistPath)")
        coroutineToast(context, context.getString(R.string.failed_to_save_playlist, filePlaylist.name))
    }
}

private suspend fun getContentUri(
    context: Context,
    filePath: String,
): Uri? {
    val bashPath = externalFileBashPath(filePath)
    val doNotUseTree = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val segments = bashPath.split(File.separatorChar)
        when {
            segments.size == 1                                                                     -> true
            segments.size == 2 && segments[0] == Environment.DIRECTORY_DOWNLOADS                   -> true
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && segments.size >= 2
                    && segments[0] == "Android" && (segments[1] == "data" || segments[1] == "obb") -> true

            else                                                                                   -> false
        }
    } else {
        false
    }
    return if (doNotUseTree) {
        getContentUriViaDocument(context, filePath)
    } else {
        getContentUriViaDocumentTree(context, filePath)
    }
}

private suspend fun getContentUriViaDocumentTree(
    context: Context,
    filePath: String,
): Uri? {
    val treeUri = chooseDirViaSAF(context, filePath)
    val documentId = run {
        val file = File(filePath)
        val storageId = file.getStorageId(context)
        val basePath = file.getBasePath()
        "$storageId:$basePath"
    }
    val childUri: Uri? = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    debug { Log.i(TAG, "Access ChildUri: $childUri") }
    return childUri
}

private suspend fun getContentUriViaDocument(
    context: Context,
    filePath: String,
): Uri? {
    val mimeTypes = arrayOf(PLAYLIST_MIME_TYPE, Playlists.CONTENT_TYPE, Playlists.ENTRY_CONTENT_TYPE)
    val documentUri = chooseFileViaSAF(context, filePath, mimeTypes)
    if (documentUri.getAbsolutePath(context) != filePath) {
        val returningPath = documentUri.getAbsolutePath(context)
        val message = buildString {
            append(context.getString(R.string.failed_to_save_playlist, filePath)).append('\n')
            append(context.getString(R.string.file_incorrect)).append('\n')
            append("Playlist($filePath) -> File($returningPath) ")
        }
        coroutineToast(context, context.getString(R.string.file_incorrect))
        warning(TAG, message)
        return null
    }
    return documentUri
}

private const val TAG = "PlaylistAppend"