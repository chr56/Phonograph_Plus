/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.loader.Albums
import player.phonograph.repo.loader.Artists
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.loaders.RecentlyPlayedTracksLoader
import player.phonograph.repo.mediastore.loaders.TopTracksLoader
import player.phonograph.service.queue.QueueManager
import player.phonograph.settings.Keys
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
import android.util.Log

object MediaItemProviders {

    abstract class MediaItemProvider {

        abstract fun browser(context: Context): List<MediaItem>

        abstract fun fetch(context: Context): List<Song>

        open fun play(context: Context): PlayRequest = PlayRequest.EmptyRequest

        /**
         * add [allItem] at the top
         */
        protected fun withPlayAllItems(resources: Resources, path: String, items: List<MediaItem>): List<MediaItem> =
            listOf(allItem(resources, path)) + items

        /**
         * MediaItem of "Play All"
         * @param path path of "all songs"
         */
        private fun allItem(resources: Resources, path: String): MediaItem =
            mediaItem(FLAG_PLAYABLE) {
                setTitle(resources.getString(R.string.action_play_all))
                setIconUri(iconRes(resources, R.drawable.ic_play_arrow_white_24dp))
                setMediaId(path)
            }

        protected fun mediaItem(flag: Int, block: MediaDescriptionCompat.Builder.() -> Unit): MediaItem =
            MediaItem(MediaDescriptionCompat.Builder().apply(block).build(), flag)

        protected fun iconRes(res: Resources, @DrawableRes resourceId: Int): Uri =
            Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(res.getResourcePackageName(resourceId))
                .appendPath(res.getResourceTypeName(resourceId))
                .appendPath(res.getResourceEntryName(resourceId))
                .build()
    }

    fun of(path: String): MediaItemProvider = when (path) {
        MEDIA_BROWSER_ROOT             -> RootProvider
        MEDIA_BROWSER_SONGS_QUEUE      -> QueueProvider
        MEDIA_BROWSER_SONGS            -> SongsProvider
        MEDIA_BROWSER_ALBUMS           -> AlbumsProvider
        MEDIA_BROWSER_ARTISTS          -> ArtistsProvider
        MEDIA_BROWSER_SONGS_FAVORITES  -> FavoriteSongsProvider
        MEDIA_BROWSER_SONGS_TOP_TRACKS -> TopTracksProvider
        MEDIA_BROWSER_SONGS_LAST_ADDED -> RecentAddedProvider
        MEDIA_BROWSER_SONGS_HISTORY    -> RecentlyPlayedProvider
        else                           -> parse(path)
    }

    private fun parse(path: String): MediaItemProvider {
        val fragments = path.split(MEDIA_BROWSER_SEPARATOR, limit = 2)


        if (fragments.size != 2) {
            Log.e(TAG, "Failed to parse: $path")
            return EmptyProvider
        }

        val type = fragments[0]
        val id = fragments[1].toLongOrNull() ?: return EmptyProvider

        return when (type) {
            MEDIA_BROWSER_ALBUMS  -> AlbumSongProvider(id)
            MEDIA_BROWSER_ARTISTS -> ArtistSongProvider(id)
            else                  -> {
                // Unknown
                Log.w(TAG, "Unknown path: $path")
                EmptyProvider
            }
        }
    }

    object EmptyProvider : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> = emptyList()
        override fun fetch(context: Context): List<Song> = emptyList()
    }

    private object RootProvider : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> = run {
            val res = context.resources
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
        }

        override fun fetch(context: Context): List<Song> = emptyList()
    }


    private object QueueProvider : MediaItemProvider() {
        private val queueManager: QueueManager get() = GlobalContext.get().get()
        override fun browser(context: Context): List<MediaItem> {
            val queue = fetch(context)
            return QueueSong.fromQueue(queue).map { it.toMediaItem() }
        }

        override fun fetch(context: Context): List<Song> = queueManager.playingQueue
    }


    private class QueueSongProvider(val index: Long) : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> = emptyList()

        override fun fetch(context: Context): List<Song> = emptyList()
    }

    private object SongsProvider : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> =
            fetch(context).map { it.toMediaItem() }

        override fun fetch(context: Context): List<Song> = Songs.all(context)
    }

    private class SongProvider(val songId: Long) : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> = emptyList()

        override fun fetch(context: Context): List<Song> = emptyList()
    }

    private object AllSongProvider : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> = emptyList()

        override fun fetch(context: Context): List<Song> = emptyList()
    }

    private object AlbumsProvider : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> =
            Albums.all(context).map { it.toMediaItem() }

        override fun fetch(context: Context): List<Song> = emptyList()
    }

    private class AlbumProvider(val albumId: Long) : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> = emptyList()

        override fun fetch(context: Context): List<Song> = emptyList()
    }

    private class AlbumSongProvider(val albumId: Long) : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                "$MEDIA_BROWSER_ALBUMS$MEDIA_BROWSER_SEPARATOR$albumId",
                fetch(context).map { it.toMediaItem() }
            )

        override fun fetch(context: Context): List<Song> = Songs.album(context, albumId)
    }

    private object ArtistsProvider : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> =
            Artists.all(context).map { it.toMediaItem() }

        override fun fetch(context: Context): List<Song> = emptyList()
    }

    private class ArtistProvider(val artistId: Long) : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> = emptyList()

        override fun fetch(context: Context): List<Song> = emptyList()
    }

    private class ArtistSongProvider(val artistId: Long) : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                "$MEDIA_BROWSER_ARTISTS$MEDIA_BROWSER_SEPARATOR$artistId",
                fetch(context).map { it.toMediaItem() }
            )

        override fun fetch(context: Context): List<Song> = Songs.artist(context, artistId)
    }

    private object FavoriteSongsProvider : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MEDIA_BROWSER_SONGS_FAVORITES,
                fetch(context).map { it.toMediaItem() }
            )

        override fun fetch(context: Context): List<Song> = FavoritesStore.get().getAllSongs(context)
    }

    private object TopTracksProvider : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MEDIA_BROWSER_SONGS_TOP_TRACKS,
                fetch(context).map { it.toMediaItem() }
            )

        override fun fetch(context: Context): List<Song> = TopTracksLoader.get().tracks(context)
    }

    private object RecentAddedProvider : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MEDIA_BROWSER_SONGS_LAST_ADDED,
                fetch(context).map { it.toMediaItem() }
            )

        private fun lastAddedCutoffTimeStamp(context: Context): Long =
            Setting(context).Composites[Keys.lastAddedCutoffTimeStamp].data / 1000

        override fun fetch(context: Context): List<Song> = Songs.since(context, lastAddedCutoffTimeStamp(context))
    }


    private object RecentlyPlayedProvider : MediaItemProvider() {
        override fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MEDIA_BROWSER_SONGS_HISTORY,
                fetch(context).map { it.toMediaItem() }
            )

        override fun fetch(context: Context): List<Song> = RecentlyPlayedTracksLoader.get().tracks(context)
    }


    private const val TAG = "MediaItemProviders"
}