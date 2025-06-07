/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.playlist.m3u


import lib.storage.launcher.IOpenFileStorageAccessible
import player.phonograph.App
import player.phonograph.R
import player.phonograph.foundation.compat.MediaStoreCompat.Audio.Playlists
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.Song
import player.phonograph.util.PLAYLIST_MIME_TYPE
import player.phonograph.util.concurrent.coroutineToast
import player.phonograph.util.file.selectContentUri
import player.phonograph.util.openOutputStreamSafe
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.IOException

object SAFPlaylistUtil {

    /**
     * @param uri content uri to write
     */
    suspend fun writePlaylist(
        context: Context,
        uri: Uri,
        songs: List<Song>,
    ): Boolean =
        openOutputStreamSafe(context, uri, "rwt")?.use { stream ->
            try {
                M3UWriter.write(stream, songs, true)
                delay(250)
                EventHub.sendEvent(App.instance, EventHub.EVENT_PLAYLISTS_CHANGED)
                true
            } catch (e: Exception) {
                warning(context, TAG, "Failed to write $uri", e)
                false
            }
        } == true


    /**
     * @param context must be [IOpenFileStorageAccessible]
     */
    suspend fun appendPlaylist(
        context: Context,
        playlistPath: String,
        songs: List<Song>,
    ) = withContext(Dispatchers.IO) {
        //
        // check
        //
        if (songs.isEmpty()) return@withContext
        require(context is IOpenFileStorageAccessible)
        require(playlistPath.contains('/'))
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

    private const val TAG = "SAFUtil"
}