/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist

import lib.phonograph.misc.ActivityResultContractUtil.chooseDirViaSAF
import lib.phonograph.misc.ActivityResultContractUtil.createFileViaSAF
import lib.phonograph.misc.IOpenFileStorageAccess
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.util.CoroutineUtil.coroutineToast
import player.phonograph.util.Util.reportError
import player.phonograph.util.Util.warning
import util.phonograph.m3u.internal.M3UGenerator
import util.phonograph.m3u.internal.appendTimestampSuffix
import androidx.documentfile.provider.DocumentFile
import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.IOException

/**
 * @param context must be [IOpenFileStorageAccess]
 */
suspend fun createPlaylistViaSAF(
    context: Context,
    playlist: Playlist,
): Unit? = createPlaylistViaSAF(
    context,
    playlist.name,
    playlist.getSongs(context),
)

/**
 * @param context must be [IOpenFileStorageAccess]
 */
suspend fun createPlaylistViaSAF(
    context: Context,
    playlistName: String,
    songs: List<Song>?,
) = withContext(Dispatchers.IO) {
    // check
    if (songs.isNullOrEmpty()) return@withContext
    require(context is IOpenFileStorageAccess)
    while (context.openFileStorageAccessTool.busy) yield()
    // launch
    val uri = createFileViaSAF(context, "$playlistName.m3u")
    openOutputStreamSafe(context, uri, "rwt")?.use { stream ->
        try {
            M3UGenerator.generate(stream, songs, true)
            coroutineToast(context, R.string.success)
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
) = createPlaylistsViaSAF(
    context,
    playlists,
    File(Environment.DIRECTORY_MUSIC)
)

/**
 * @param context must be [IOpenFileStorageAccess]
 */
suspend fun createPlaylistsViaSAF(
    context: Context,
    playlists: List<Playlist>,
    initialPosition: File,
) = withContext(Dispatchers.IO) {
    // check
    if (playlists.isEmpty()) return@withContext
    require(context is IOpenFileStorageAccess)
    while (context.openFileStorageAccessTool.busy) yield()
    // launch
    coroutineToast(
        context,
        context.getString(R.string.direction_open_folder_with_saf),
        true
    )
    val treeUri = chooseDirViaSAF(context, initialPosition)
    val dir = DocumentFile.fromTreeUri(context, treeUri)
    if (dir != null && dir.isDirectory) {
        for (playlist in playlists) {
            val file = dir.createFile(
                "audio/x-mpegurl",
                appendTimestampSuffix(playlist.name)
            )
            if (file != null) {
                openOutputStreamSafe(context, file.uri, "rwt")?.use { outputStream ->
                    val songs: List<Song> = playlist.getSongs(context)
                    try {
                        M3UGenerator.generate(outputStream, songs, true)
                    } catch (e: IOException) {
                        reportError(e, TAG, "")
                        coroutineToast(context, R.string.failed)
                    }
                }
            } else {
                warning(
                    TAG, context.getString(
                        R.string.failed_to_save_playlist,
                        playlist.name
                    )
                )
            }
        }
        coroutineToast(context, R.string.success)
    } else {
        warning(
            TAG, "${context.getString(R.string.failed)}: uri $treeUri"
        )
    }
}


private const val TAG = "PlaylistCreate"
