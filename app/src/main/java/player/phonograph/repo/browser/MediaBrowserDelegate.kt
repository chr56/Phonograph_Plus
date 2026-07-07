/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo.browser

import player.phonograph.model.PlayRequest
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaLibraryService.LibraryParams
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.os.Process
import android.provider.MediaStore
import android.util.Log

object MediaBrowserDelegate {
    private const val TAG = "MediaBrowser"

    fun root(context: Context, clientPackageName: String, clientUid: Int, params: LibraryParams?): MediaItem? {
        if (!validate(context, clientPackageName, clientUid)) return null
        val path = when {
            params?.isRecent == true -> MediaItemPath.pageLastAdded
            params?.isSuggested == true -> MediaItemPath.pageTopTracks
            else -> MediaItemPath.root
        }
        return MediaItem.Builder().setMediaId(path.mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(context.applicationInfo.loadLabel(context.packageManager))
                    .setIsBrowsable(true).setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            ).build()
    }

    suspend fun listChildren(path: String, context: Context): List<MediaItem> =
        MediaItemProviders.of(path).browser(context)

    suspend fun item(context: Context, mediaId: String): MediaItem? {
        val path = MediaBrowserTree.resolve(mediaId) ?: return null
        if (path.segments.isEmpty()) return root(context, context.packageName, Process.myUid(), null)
        val parent = MediaItemPath(path.segments.dropLast(1), null)
        return listChildren(parent.mediaId, context).find { it.mediaId == mediaId }
    }

    suspend fun playFromMediaId(context: Context, mediaId: String, @Suppress("UNUSED_PARAMETER") extras: Bundle?): PlayRequest =
        MediaItemProviders.of(mediaId).play(context)

    suspend fun playFromSearch(context: Context, query: String?, extras: Bundle?): PlayRequest.SongsRequest =
        if (query.isNullOrEmpty()) {
            PlayRequest.SongsRequest(Songs.all(context, Settings(context)[Keys.songSortMode].read()), 0)
        } else {
            val title = extras?.getString(MediaStore.EXTRA_MEDIA_TITLE)
            val album = extras?.getString(MediaStore.EXTRA_MEDIA_ALBUM)
            val artist = extras?.getString(MediaStore.EXTRA_MEDIA_ARTIST)
            val songs = if (title != null || album != null || artist != null) {
                MediaStoreSongs.search(context, extras.getString(SearchManager.QUERY) ?: query, title, album, artist)
            } else {
                Songs.searchByTitle(context, query)
            }
            PlayRequest.SongsRequest(songs, 0)
        }

    // todo: validate package names & signatures
    private fun validate(context: Context, clientPackageName: String, clientUid: Int): Boolean {
        return if (clientUid == Process.SYSTEM_UID) {
            true
        } else if (checkPackageName(clientPackageName)) {
            if (checkSignatures(context, clientPackageName)) {
                true
            } else {
                Log.e(TAG, "Invalidate Signature of $clientPackageName")
                false
            }
        } else {
            Log.e(TAG, "Unknown: $clientPackageName")
            false
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun checkPackageName(clientPackageName: String): Boolean = true

    @Suppress("UNUSED_PARAMETER")
    private fun checkSignatures(context: Context, clientPackageName: String): Boolean = true
}
