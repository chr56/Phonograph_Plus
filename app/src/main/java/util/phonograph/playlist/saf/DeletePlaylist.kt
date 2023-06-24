/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.saf

import lib.phonograph.storage.getBasePath
import lib.phonograph.uri.isTreeDocumentFileSafe
import player.phonograph.R
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.util.coroutineToast
import player.phonograph.util.warning
import androidx.documentfile.provider.DocumentFile
import android.content.Context
import android.util.Log


/**
 * use SAF to choose a directory, and analysis, then return list of [DocumentFile] that presenting Playlist to delete
 * @param context must be IOpenDirStorageAccess
 * @return list of [DocumentFile] to delete
 */
suspend fun searchPlaylistsForDeletionViaSAF(
    context: Context,
    filePlaylists: List<FilePlaylist>,
): List<DocumentFile> {
    // check
    if (filePlaylists.isEmpty()) return emptyList()
    // open with SAF to get tree uri
    val playlistPaths = filePlaylists.map { it.associatedFilePath }
    val treeUri = chooseCommonDirViaSAF(context, playlistPaths) ?: return emptyList()
    val folder = if (treeUri.isTreeDocumentFileSafe()) DocumentFile.fromTreeUri(context, treeUri) else null
    // valid
    if (folder == null) {
        warning(TAG, "Invalid Uri: $treeUri")
        return emptyList()
    }
    val searchedPlaylist = searchPlaylist(context, folder, filePlaylists) // search playlist in folder

    return if (searchedPlaylist.isNotEmpty()) {
        analysis(context, playlistPaths, searchedPlaylist) //todo
    } else {
        coroutineToast(context, "${context.getString(R.string.failed)}\nCan not locate files.")
        emptyList()
    }
}


/**
 * @return list for deletion
 */
private fun analysis(
    context: Context,
    playlistPathsToDelete: List<String>,
    searchedPlaylist: List<DocumentFile>,
): List<DocumentFile> {
    return searchedPlaylist.filter {
        val name = it.getBasePath(context) ?: it.name ?: ""
        name.endsWith("m3u", ignoreCase = true) || name.endsWith("m3u8", ignoreCase = true)
    }
}


private fun searchPlaylist(
    context: Context,
    root: DocumentFile,
    filePlaylists: List<FilePlaylist>,
): List<DocumentFile> {
    val fileNames = filePlaylists.map { it.associatedFilePath }
    return if (fileNames.isEmpty()) {
        Log.w(TAG, "No playlist display name?")
        emptyList()
    } else
        searchFiles(context, root, fileNames)
}

private fun searchFiles(
    context: Context,
    root: DocumentFile,
    filenames: List<String>,
): List<DocumentFile> {
    return if (!root.isDirectory) emptyList()
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
        result
    }
}

private const val TAG = "PlaylistDelete"