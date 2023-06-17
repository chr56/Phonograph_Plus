/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.saf

import lib.phonograph.misc.ActivityResultContractUtil.chooseDirViaSAF
import lib.phonograph.misc.IOpenDirStorageAccess
import lib.phonograph.storage.getBasePath
import lib.phonograph.uri.isTreeDocumentFileSafe
import player.phonograph.R
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.util.coroutineToast
import player.phonograph.mechanism.PlaylistsManagement
import player.phonograph.util.warning
import androidx.documentfile.provider.DocumentFile
import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File


/**
 * use SAF to choose a directory, and analysis, then return list of [DocumentFile] that presenting Playlist to delete
 * @param context must be IOpenDirStorageAccess
 * @return list of [DocumentFile] to delete
 */
suspend fun tryToDeletePlaylistsViaSAF(
    context: Context,
    filePlaylists: List<FilePlaylist>,
): List<DocumentFile> = withContext(Dispatchers.IO) {
    // check
    if (filePlaylists.isEmpty()) return@withContext emptyList()
    require(context is IOpenDirStorageAccess)
    while (context.openDirStorageAccessTool.busy) yield()
    // common root
    val playlistPaths = filePlaylists.map { it.associatedFilePath }
    val commonRoot = commonPathRoot(playlistPaths)
    coroutineToast(
        context,
        context.getString(R.string.direction_open_folder_with_saf),
        true
    )
    // launch
    val commonRootFile = if (commonRoot.isNotEmpty()) File(commonRoot) else Environment.getExternalStorageDirectory()
    val treeUri = chooseDirViaSAF(context, commonRootFile)
    val folder =
        if (treeUri.isTreeDocumentFileSafe()) DocumentFile.fromTreeUri(context, treeUri) else null
    // valid
    if (folder == null) {
        warning(TAG, "Invalid Uri: $treeUri")
        return@withContext emptyList()
    }
    val searchedPlaylist = searchPlaylist(context, folder, filePlaylists) // search playlist in folder

    return@withContext if (searchedPlaylist.isNotEmpty()) {
        analysis(context, playlistPaths, searchedPlaylist) //todo
    } else {
        coroutineToast(context, R.string.failed)
        emptyList()
    }
}


/**
 * @return list for deletion
 */
private fun analysis(
    context: Context,
    playlistPathsToDelete: List<String>,
    searchedPlaylist: List<DocumentFile>
): List<DocumentFile> {
    return searchedPlaylist.filter {
        val name = it.getBasePath(context) ?: it.name ?: ""
        name.endsWith("m3u", ignoreCase = true) || name.endsWith("m3u8", ignoreCase = true)
    }
}


private fun searchPlaylist(
    context: Context,
    root: DocumentFile,
    filePlaylists: List<FilePlaylist>
): List<DocumentFile> {
    val fileNames = PlaylistsManagement.getPlaylistFileNames(context, filePlaylists)
    return if (fileNames.isEmpty()) {
        Log.w(TAG, "No playlist display name?")
        emptyList()
    } else
        searchFiles(context, root, fileNames)
}

private fun searchFiles(
    context: Context,
    root: DocumentFile,
    filenames: List<String>
): List<DocumentFile> {
    if (!root.isDirectory)
        return ArrayList()
    else {
        val result = mutableListOf<DocumentFile>()
        for (sub in root.listFiles()) {
            if (sub.isFile) {
                filenames.forEach { name ->
                    if (name == sub.name.orEmpty()) result.add(sub)
                }
            }
            if (sub.isDirectory) {
                result.addAll(
                    searchFiles(context, sub, filenames)
                )
            }
        }
        return result
    }
}

private const val TAG = "PlaylistDelete"