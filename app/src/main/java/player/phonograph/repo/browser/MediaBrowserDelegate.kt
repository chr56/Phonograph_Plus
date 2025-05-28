/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

import player.phonograph.model.PlayRequest
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import androidx.media.MediaBrowserServiceCompat.BrowserRoot
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.os.Process
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.util.Log

object MediaBrowserDelegate {
    private const val TAG = "MediaBrowser"

    fun onGetRoot(context: Context, clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? =
        if (validate(context, clientPackageName, clientUid)) {
            val root = if (rootHints == null) {
                MediaItemPath.ROOT_PATH
            } else {
                when {
                    rootHints.getBoolean(BrowserRoot.EXTRA_RECENT)    -> MediaItemPath.pageLastAdded.mediaId
                    rootHints.getBoolean(BrowserRoot.EXTRA_SUGGESTED) -> MediaItemPath.pageTopTracks.mediaId
                    else                                              -> MediaItemPath.ROOT_PATH
                }
            }
            BrowserRoot(root, null)
        } else {
            null
        }

    suspend fun listChildren(path: String, context: Context): List<MediaBrowserCompat.MediaItem> =
        MediaItemProviders.of(path).browser(context)

    suspend fun playFromMediaId(context: Context, mediaId: String, @Suppress("UNUSED_PARAMETER") extras: Bundle?): PlayRequest =
        MediaItemProviders.of(mediaId).play(context)

    suspend fun playFromSearch(context: Context, query: String?, extras: Bundle?): PlayRequest.SongsRequest =
        if (query.isNullOrEmpty()) {
            PlayRequest.SongsRequest(Songs.all(context), 0)
        } else {
            if (extras != null) {
                val query = extras.getString(SearchManager.QUERY)
                val title = extras.getString(MediaStore.EXTRA_MEDIA_TITLE)
                val album = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                val artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                val songs = MediaStoreSongs.search(context, query, title, album, artist)
                PlayRequest.SongsRequest(songs, 0)
            } else {
                val songs = Songs.searchByTitle(context, query)
                PlayRequest.SongsRequest(songs, 0)
            }
        }

    fun error(context: Context): List<MediaBrowserCompat.MediaItem> = listOf(MediaItemProviders.error(context))

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
    private fun checkPackageName(clientPackageName: String): Boolean {
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    private fun checkSignatures(context: Context, clientPackageName: String): Boolean {
        // fetchPackageSignatures(context, clientPackageName)
        return true
    }

    private fun lastAddedCutoffTimeStamp(context: Context): Long =
        Setting(context)[Keys.lastAddedCutoffTimeStamp].data / 1000
}