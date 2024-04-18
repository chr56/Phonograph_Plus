/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist.saf

import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import lib.activityresultcontract.IOpenFileStorageAccess
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.util.PLAYLIST_MIME_TYPE
import player.phonograph.util.coroutineToast
import player.phonograph.util.file.selectContentUri
import player.phonograph.util.openOutputStreamSafe
import player.phonograph.util.warning
import player.phonograph.mechanism.playlist.m3u.M3UWriter
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
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
        warning(TAG, "Failed write playlist via uri: $uri (from file $playlistPath)")
        coroutineToast(context, context.getString(R.string.failed_to_save_playlist, filePlaylist.name))
    }
}

private const val TAG = "PlaylistAppend"