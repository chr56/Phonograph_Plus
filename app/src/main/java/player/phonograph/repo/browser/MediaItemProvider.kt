/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.model.QueueSong
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.mediastore.loaders.AlbumLoader
import player.phonograph.repo.mediastore.loaders.ArtistLoader
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.repo.mediastore.loaders.dynamics.TopAndRecentlyPlayedTracksLoader
import player.phonograph.service.queue.QueueManager
import player.phonograph.settings.Setting
import androidx.annotation.DrawableRes
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat

object MediaItemProvider {

    fun browseRoot(res: Resources): List<MediaItem> =
        listOf(
            mediaItem(FLAG_BROWSABLE) {
                setTitle(res.getString(R.string.songs))
                setIconUri(iconRes(res, R.drawable.ic_music_note_white_24dp))
                setMediaId(MEDIA_BROWSER_SONGS)
            },
            mediaItem(FLAG_BROWSABLE) {
                setTitle(res.getString(R.string.albums))
                setIconUri(iconRes(res, R.drawable.ic_album_white_24dp))
                setMediaId(MEDIA_BROWSER_ALBUMS)
            },
            mediaItem(FLAG_BROWSABLE) {
                setTitle(res.getString(R.string.artists))
                setIconUri(iconRes(res, R.drawable.ic_person_white_24dp))
                setMediaId(MEDIA_BROWSER_ARTISTS)
            },
            mediaItem(FLAG_BROWSABLE) {
                setTitle(res.getString(R.string.label_playing_queue))
                setIconUri(iconRes(res, R.drawable.ic_queue_music_white_24dp))
                setMediaId(MEDIA_BROWSER_SONGS_QUEUE)
            },
            mediaItem(FLAG_BROWSABLE) {
                setTitle(res.getString(R.string.favorites))
                setIconUri(iconRes(res, R.drawable.ic_favorite_white_24dp))
                setMediaId(MEDIA_BROWSER_SONGS_FAVORITES)
            },
            mediaItem(FLAG_BROWSABLE) {
                setTitle(res.getString(R.string.my_top_tracks))
                setIconUri(iconRes(res, R.drawable.ic_trending_up_white_24dp))
                setMediaId(MEDIA_BROWSER_SONGS_TOP_TRACKS)
            },
            mediaItem(FLAG_BROWSABLE) {
                setTitle(res.getString(R.string.last_added))
                setIconUri(iconRes(res, R.drawable.ic_library_add_white_24dp))
                setMediaId(MEDIA_BROWSER_SONGS_LAST_ADDED)
            },
            mediaItem(FLAG_BROWSABLE) {
                setTitle(res.getString(R.string.history))
                setIconUri(iconRes(res, R.drawable.ic_access_time_white_24dp))
                setMediaId(MEDIA_BROWSER_SONGS_HISTORY)
            },
        )


    fun browseQueue(): List<MediaItem> {
        val queueManager: QueueManager = GlobalContext.get().get()
        val queue = queueManager.playingQueue
        return QueueSong.fromQueue(queue).map { it.toMediaItem() }
    }

    fun browseSongs(context: Context): List<MediaItem> {
        return SongLoader.all(context).map { it.toMediaItem() }
    }

    fun browseAlbums(context: Context): List<MediaItem> {
        return AlbumLoader.all(context).map { it.toMediaItem() }
    }

    fun browseAlbum(context: Context, id: Long): List<MediaItem> {
        return mutableListOf(albumAllItem(context.resources, id)) +
                AlbumLoader.id(context, id).songs.map { it.toMediaItem() }
    }

    fun browseArtists(context: Context): List<MediaItem> {
        return ArtistLoader.all(context).map { it.toMediaItem() }
    }

    fun browseArtist(context: Context, id: Long): List<MediaItem> {
        return mutableListOf(artistAllItem(context.resources, id)) +
                ArtistLoader.id(context, id).songs.map { it.toMediaItem() }
    }

    fun browseFavorite(context: Context): List<MediaItem> {
        return listOf(selectAllItem(context.resources, MEDIA_BROWSER_SONGS_FAVORITES)) +
                FavoritesStore.get().getAllSongs(context).map { it.toMediaItem() }
    }

    fun browseMyTopTrack(context: Context): List<MediaItem> {
        return listOf(selectAllItem(context.resources, MEDIA_BROWSER_SONGS_FAVORITES)) +
                TopAndRecentlyPlayedTracksLoader.topTracks(context).map { it.toMediaItem() }
    }

    fun browseLastAdded(context: Context): List<MediaItem> {
        return listOf(selectAllItem(context.resources, MEDIA_BROWSER_SONGS_FAVORITES)) +
                SongLoader.since(context, Setting.instance.lastAddedCutoff).map { it.toMediaItem() }
    }

    fun browseHistory(context: Context): List<MediaItem> {
        return listOf(selectAllItem(context.resources, MEDIA_BROWSER_SONGS_FAVORITES)) +
                TopAndRecentlyPlayedTracksLoader.recentlyPlayedTracks(context).map { it.toMediaItem() }
    }

    private fun albumAllItem(resources: Resources, id: Long): MediaItem =
        selectAllItem(
            resources, "$MEDIA_BROWSER_ALBUMS$MEDIA_BROWSER_SEPARATOR$id"
        )

    private fun artistAllItem(resources: Resources, id: Long): MediaItem =
        selectAllItem(
            resources, "$MEDIA_BROWSER_ARTISTS$MEDIA_BROWSER_SEPARATOR$id"
        )

    private fun selectAllItem(resources: Resources, path: String): MediaItem =
        mediaItem(FLAG_PLAYABLE) {
            setTitle(resources.getString(R.string.action_play_all))
            setIconUri(iconRes(resources, R.drawable.ic_play_arrow_white_24dp))
            setMediaId(path)
        }

    private fun mediaItem(flag: Int, block: MediaDescriptionCompat.Builder.() -> Unit): MediaItem {
        return MediaItem(MediaDescriptionCompat.Builder().apply(block).build(), flag)
    }

    private fun iconRes(res: Resources, @DrawableRes resourceId: Int): Uri =
        Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(res.getResourcePackageName(resourceId))
            .appendPath(res.getResourceTypeName(resourceId))
            .appendPath(res.getResourceEntryName(resourceId))
            .build()
}