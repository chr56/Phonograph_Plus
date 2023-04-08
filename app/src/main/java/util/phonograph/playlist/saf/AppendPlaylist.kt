/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.saf

import legacy.phonograph.MediaStoreCompat.Audio.Playlists.CONTENT_TYPE
import legacy.phonograph.MediaStoreCompat.Audio.Playlists.ENTRY_CONTENT_TYPE
import lib.phonograph.misc.ActivityResultContractUtil.chooseFileViaSAF
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.storage.getAbsolutePath
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.util.CoroutineUtil.coroutineToast
import player.phonograph.mechanism.PlaylistsManagement
import player.phonograph.util.Util.reportError
import util.phonograph.playlist.m3u.M3UGenerator
import android.content.Context
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
    playlistId: Long
) = appendToPlaylistViaSAF(
    context,
    songs,
    PlaylistsManagement.getPlaylist(context, playlistId),
)

/**
 * @param context must be [IOpenFileStorageAccess]
 */
suspend fun appendToPlaylistViaSAF(
    context: Context,
    songs: List<Song>,
    filePlaylist: FilePlaylist
) = withContext(Dispatchers.IO) {
    // check
    if (songs.isEmpty()) return@withContext
    require(context is IOpenFileStorageAccess)
    while (context.openFileStorageAccessTool.busy) yield()
    // config
    val playlistPath = PlaylistsManagement.getPlaylistPath(context, filePlaylist)
    val mimeTypes = arrayOf("audio/x-mpegurl", CONTENT_TYPE, ENTRY_CONTENT_TYPE)
    // launch
    val uri = chooseFileViaSAF(context, File(playlistPath), mimeTypes)
    // check
    if (!checkUri(context, filePlaylist, uri)) {
        val returningPath = uri.getAbsolutePath(context)
        val message = buildString {
            append(context.getString(R.string.failed_to_save_playlist, filePlaylist.name)).append('\n')
            append(context.getString(R.string.file_incorrect)).append('\n')
            append("Playlist($playlistPath) -> File($returningPath) ")
        }
        reportError(
            IllegalStateException(message), TAG, message
        )
        return@withContext
    }
    // write
    try {
        openOutputStreamSafe(context, uri, "wa")?.use { outputStream ->
            M3UGenerator.generate(outputStream, songs, false)
            coroutineToast(context, context.getString(R.string.success))
        }
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

private const val TAG = "PlaylistAppend"