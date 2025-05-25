/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist.saf

import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import lib.storage.launcher.IOpenFileStorageAccessible
import player.phonograph.R
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.playlist.m3u.M3UWriter
import player.phonograph.model.Song
import player.phonograph.util.PLAYLIST_MIME_TYPE
import player.phonograph.util.concurrent.coroutineToast
import player.phonograph.util.file.selectContentUri
import player.phonograph.util.openOutputStreamSafe
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.IOException

/**
 * @param context must be [IOpenFileStorageAccessible]
 */
suspend fun appendToPlaylistViaSAF(
    context: Context,
    songs: List<Song>,
    playlistId: Long,
    playlistPath: String,
) = withContext(Dispatchers.IO) {
    //
    // check
    //
    if (songs.isEmpty()) return@withContext
    require(context is IOpenFileStorageAccessible)
    require(playlistId > 0 || playlistPath.contains('/'))
    while (context.openFileStorageAccessDelegate.busy) yield()
    //
    // select
    //
    val mimeTypes = arrayOf(PLAYLIST_MIME_TYPE, Playlists.CONTENT_TYPE, Playlists.ENTRY_CONTENT_TYPE)
    val uri: Uri? = selectContentUri(context, playlistPath, mimeTypes)
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
        warning(context, TAG, "Failed write playlist via uri: $uri (from file $playlistPath)")
        coroutineToast(context, context.getString(R.string.err_failed_to_save_playlist, playlistPath))
    }
}

private const val TAG = "PlaylistAppend"