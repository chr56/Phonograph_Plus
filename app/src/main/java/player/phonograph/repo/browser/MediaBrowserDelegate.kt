/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

import player.phonograph.model.Song
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.mediastore.loaders.AlbumLoader
import player.phonograph.repo.mediastore.loaders.ArtistLoader
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.repo.mediastore.loaders.TopAndRecentlyPlayedTracksLoader
import player.phonograph.repo.mediastore.processQuery
import player.phonograph.service.MusicPlayerRemote
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
                MEDIA_BROWSER_ROOT
            } else {
                when {
                    rootHints.getBoolean(BrowserRoot.EXTRA_RECENT)    -> MEDIA_BROWSER_SONGS_LAST_ADDED
                    rootHints.getBoolean(BrowserRoot.EXTRA_SUGGESTED) -> MEDIA_BROWSER_SONGS_TOP_TRACKS
                    else                                              -> MEDIA_BROWSER_ROOT
                }
            }
            BrowserRoot(root, null)
        } else {
            null
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

                val fragments = path.split(MEDIA_BROWSER_SEPARATOR, limit = 2)

                if (fragments.size != 2) {
                    Log.e(TAG, "Failed to parse: $path")
                    return emptyList()
                }

                val type = fragments[0]
                val id = fragments[1].toLongOrNull() ?: return emptyList()

                when (type) {
                    MEDIA_BROWSER_ALBUMS  -> MediaItemProvider.browseAlbum(context, id)
                    MEDIA_BROWSER_ARTISTS -> MediaItemProvider.browseArtist(context, id)
                    else                  -> {
                        // Unknown
                        Log.w(TAG, "Unknown path: $path")
                        emptyList()
                    }
                }
            }
        }
    }

    fun playFromMediaId(context: Context, mediaId: String, @Suppress("UNUSED_PARAMETER") extras: Bundle?): List<Song> {
        return when (mediaId) {
            MEDIA_BROWSER_SONGS_FAVORITES  -> FavoritesStore.get().getAllSongs(context)
            MEDIA_BROWSER_SONGS_TOP_TRACKS -> TopAndRecentlyPlayedTracksLoader.get().topTracks(context)
            MEDIA_BROWSER_SONGS_LAST_ADDED -> SongLoader.since(context, Setting.instance.lastAddedCutoff)
            MEDIA_BROWSER_SONGS_HISTORY    -> TopAndRecentlyPlayedTracksLoader.get().recentlyPlayedTracks(context)

            else                           -> {
                val fragments = mediaId.split(MEDIA_BROWSER_SEPARATOR, limit = 2)

                if (fragments.size != 2) {
                    Log.e(TAG, "Failed to parse: $mediaId")
                    return emptyList()
                }

                val type = fragments[0]
                val id = fragments[1].toLongOrNull() ?: return emptyList()

                when (type) {
                    MEDIA_BROWSER_SONGS       -> listOf(SongLoader.id(context, id))
                    MEDIA_BROWSER_ALBUMS      -> AlbumLoader.id(context, id).songs
                    MEDIA_BROWSER_ARTISTS     -> ArtistLoader.id(context, id).songs

                    MEDIA_BROWSER_SONGS_QUEUE -> {
                        MusicPlayerRemote.playSongAt(id.toInt())
                        emptyList()
                    }

                    else                      -> emptyList()
                }
            }
        }
    }

    fun playFromSearch(context: Context, query: String?, extras: Bundle?): List<Song> {
        return if (query.isNullOrEmpty()) {
            SongLoader.all(context)
        } else {
            if (extras != null) {
                processQuery(context, extras)
            } else {
                SongLoader.searchByTitle(context, query)
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
}