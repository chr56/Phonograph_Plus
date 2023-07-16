/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.browser

import androidx.media.MediaBrowserServiceCompat.BrowserRoot
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.util.Log



object MediaBrowserDelegate {
    private const val TAG = "MediaBrowser"

    fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        // todo: validate package
        return BrowserRoot(MEDIA_BROWSER_ROOT, null)
    }

    fun listChildren(path: String, context: Context): List<MediaBrowserCompat.MediaItem> {
        return when (path) {
            MEDIA_BROWSER_ROOT             -> MediaItemProvider.browseRoot(context.resources)
            MEDIA_BROWSER_SONGS_QUEUE      -> MediaItemProvider.browseQueue()
            MEDIA_BROWSER_SONGS            -> MediaItemProvider.browseSongs(context)
            MEDIA_BROWSER_ALBUMS           -> MediaItemProvider.browseAlbums(context)
            MEDIA_BROWSER_ARTISTS          -> MediaItemProvider.browseArtists(context)
            MEDIA_BROWSER_SONGS_FAVORITES  -> MediaItemProvider.browseFavorite(context)
            MEDIA_BROWSER_SONGS_TOP_TRACKS -> MediaItemProvider.browseMyTopTrack(context)
            MEDIA_BROWSER_SONGS_LAST_ADDED -> MediaItemProvider.browseLastAdded(context)
            MEDIA_BROWSER_SONGS_HISTORY    -> MediaItemProvider.browseHistory(context)
            else                           -> {
                // Unknown
                Log.w(TAG, "Unknown path: $path")
                emptyList()
            }
        }
    }

    fun playFromMediaId(mediaId: String?, extras: Bundle?): Any? {
        if (mediaId == null) return null

        val fragments = mediaId.split(MEDIA_BROWSER_SEPARATOR, limit = 2)


        return Any()
    }
}