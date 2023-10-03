/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

import player.phonograph.model.Song
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.processQuery
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import androidx.media.MediaBrowserServiceCompat.BrowserRoot
import android.content.Context
import android.os.Bundle
import android.os.Process
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

    fun listChildren(path: String, context: Context): List<MediaBrowserCompat.MediaItem> =
        MediaItemProviders.of(path).browser(context)

    fun playFromMediaId(context: Context, mediaId: String, @Suppress("UNUSED_PARAMETER") extras: Bundle?): List<Song> {
        return when (val request = MediaItemProviders.of(mediaId).play(context)) {
            PlayRequest.EmptyRequest     -> emptyList()

            is PlayRequest.PlayAtRequest -> {
                MusicPlayerRemote.playSongAt(request.index)
                emptyList()
            }

            is PlayRequest.SongRequest   -> {
                listOf(request.song)
            }
            is PlayRequest.SongsRequest  -> {
                request.songs
            }
        }
    }

    fun playFromSearch(context: Context, query: String?, extras: Bundle?): List<Song> {
        return if (query.isNullOrEmpty()) {
            Songs.all(context)
        } else {
            if (extras != null) {
                processQuery(context, extras)
            } else {
                Songs.searchByTitle(context, query)
            }
        }
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
    private fun checkPackageName(clientPackageName: String): Boolean {
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    private fun checkSignatures(context: Context, clientPackageName: String): Boolean {
        // fetchPackageSignatures(context, clientPackageName)
        return true
    }

    private fun lastAddedCutoffTimeStamp(context: Context): Long =
        Setting(context).Composites[Keys.lastAddedCutoffTimeStamp].data / 1000
}