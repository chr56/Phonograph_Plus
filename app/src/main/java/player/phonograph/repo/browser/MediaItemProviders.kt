/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.PlayRequest
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.repo.loader.Albums
import player.phonograph.repo.loader.Artists
import player.phonograph.repo.loader.Genres
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.repo.mediastore.loaders.RecentlyPlayedTracksLoader
import player.phonograph.repo.mediastore.loaders.TopTracksLoader
import player.phonograph.service.queue.QueueManager
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.mediaStoreAlbumArtUri
import androidx.annotation.DrawableRes
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat

object MediaItemProviders {

    /**
     * Present a MediaItem
     */
    interface MediaItemProvider {
        suspend fun browser(context: Context): List<MediaItem>
        suspend fun play(context: Context): PlayRequest
    }


    fun of(path: String): MediaItemProvider = parse(path)


    abstract class AbsMediaItemProvider : MediaItemProvider {

        override suspend fun browser(context: Context): List<MediaItem> = emptyList()

        override suspend fun play(context: Context): PlayRequest = PlayRequest.EmptyRequest

        /**
         * add [allItem] at the top
         */
        protected fun withPlayAllItems(resources: Resources, path: String, items: List<MediaItem>): List<MediaItem> =
            listOf(
                allItem(resources, path)
            ) + items

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

        /**
         * create a MediaItem
         */
        protected fun mediaItem(flag: Int, block: MediaDescriptionCompat.Builder.() -> Unit): MediaItem =
            MediaItem(MediaDescriptionCompat.Builder().apply(block).build(), flag)

        protected fun iconRes(res: Resources, @DrawableRes resourceId: Int): Uri =
            Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(res.getResourcePackageName(resourceId))
                .appendPath(res.getResourceTypeName(resourceId))
                .appendPath(res.getResourceEntryName(resourceId))
                .build()

        protected fun Song.toMediaItem(): MediaItem =
            mediaItem(FLAG_PLAYABLE) {
                setTitle(title)
                setSubtitle(albumName)
                setDescription(artistName)
                setIconUri(mediaStoreAlbumArtUri(albumId))
                setMediaId(MediaItemPath.song(id).mediaId)
            }

        protected fun QueueSong.toMediaItem(): MediaItem =
            mediaItem(FLAG_PLAYABLE) {
                setTitle(song.title)
                setSubtitle(song.albumName)
                setDescription(song.artistName)
                setMediaId(MediaItemPath.queueSong(index).mediaId)
            }

        protected fun Album.toMediaItem(): MediaItem =
            mediaItem(FLAG_BROWSABLE) {
                setTitle(title)
                setSubtitle(artistName)
                setDescription(artistName)
                setIconUri(mediaStoreAlbumArtUri(id))
                setMediaId(MediaItemPath.album(id).mediaId)
            }

        protected fun Artist.toMediaItem(): MediaItem =
            mediaItem(FLAG_BROWSABLE) {
                setTitle(name)
                setMediaId(MediaItemPath.artist(id).mediaId)
            }

        protected fun Playlist.toMediaItem(): MediaItem =
            mediaItem(FLAG_BROWSABLE) {
                setTitle(name)
                setMediaId(MediaItemPath.playlist(id()).mediaId)
            }

        protected fun Genre.toMediaItem(): MediaItem =
            mediaItem(FLAG_BROWSABLE) {
                setTitle(name)
                setSubtitle(songCount.toString())
                setMediaId(MediaItemPath.genre(id).mediaId)
            }
    }

    private fun parse(path: String): MediaItemProvider {
        val mediaItemPath = MediaBrowserTree.resolve(path) ?: return EmptyProvider

        val segments = mediaItemPath.segments

        return when (segments.size) {
            0    -> RootProvider

            1    -> when (segments[0]) {
                MediaItemPath.SONGS            -> SongsProvider
                MediaItemPath.ALBUMS           -> AlbumsProvider
                MediaItemPath.ARTISTS          -> ArtistsProvider
                MediaItemPath.SONGS_QUEUE      -> QueueProvider
                MediaItemPath.PLAYLISTS        -> PlaylistsProvider
                MediaItemPath.SONGS_FAVORITES  -> FavoriteSongsProvider
                MediaItemPath.SONGS_TOP_TRACKS -> TopTracksProvider
                MediaItemPath.SONGS_LAST_ADDED -> RecentAddedProvider
                MediaItemPath.SONGS_HISTORY    -> RecentlyPlayedProvider
                MediaItemPath.GENRES           -> GenresProvider
                else                           -> EmptyProvider
            }

            else -> {
                when (segments[0]) {
                    MediaItemPath.SONGS            -> {
                        val item = segments[1]
                        if (item == MediaItemPath.PLAY_ALL) {
                            SongsProvider
                        } else {
                            SongProvider(item.toLong())
                        }
                    }

                    MediaItemPath.ALBUMS           -> {
                        val item = segments[1]
                        AlbumProvider(item.toLong())
                    }

                    MediaItemPath.ARTISTS          -> {
                        val item = segments[1]
                        ArtistProvider(item.toLong())
                    }

                    MediaItemPath.SONGS_QUEUE      -> {
                        val item = segments[1]
                        QueueSongProvider(item.toInt())
                    }

                    MediaItemPath.PLAYLISTS        -> {
                        val item = segments[1]
                        PlaylistProvider(item.toLong())
                    }

                    MediaItemPath.SONGS_FAVORITES  -> {
                        if (segments[1] == MediaItemPath.PLAY_ALL) FavoriteSongsProvider else EmptyProvider
                    }

                    MediaItemPath.SONGS_TOP_TRACKS -> {
                        if (segments[1] == MediaItemPath.PLAY_ALL) TopTracksProvider else EmptyProvider
                    }

                    MediaItemPath.SONGS_LAST_ADDED -> {
                        if (segments[1] == MediaItemPath.PLAY_ALL) RecentAddedProvider else EmptyProvider
                    }

                    MediaItemPath.SONGS_HISTORY    -> {
                        if (segments[1] == MediaItemPath.PLAY_ALL) RecentlyPlayedProvider else EmptyProvider
                    }

                    MediaItemPath.GENRES           -> {
                        val item = segments[1]
                        GenreProvider(item.toLong())
                    }

                    else                           -> EmptyProvider
                }
            }

        }
    }


    object EmptyProvider : MediaItemProvider {
        override suspend fun browser(context: Context): List<MediaItem> = emptyList()
        override suspend fun play(context: Context): PlayRequest = PlayRequest.EmptyRequest
    }

    private object RootProvider : AbsMediaItemProvider() {
        override suspend fun browser(context: Context): List<MediaItem> = run {
            val res = context.resources
            listOf(
                mediaItem(FLAG_BROWSABLE) {
                    setTitle(res.getString(R.string.songs))
                    setIconUri(iconRes(res, R.drawable.ic_music_note_white_24dp))
                    setMediaId(MediaItemPath.pageSongs.mediaId)
                },
                mediaItem(FLAG_BROWSABLE) {
                    setTitle(res.getString(R.string.albums))
                    setIconUri(iconRes(res, R.drawable.ic_album_white_24dp))
                    setMediaId(MediaItemPath.pageAlbums.mediaId)
                },
                mediaItem(FLAG_BROWSABLE) {
                    setTitle(res.getString(R.string.artists))
                    setIconUri(iconRes(res, R.drawable.ic_person_white_24dp))
                    setMediaId(MediaItemPath.pageArtist.mediaId)
                },
                mediaItem(FLAG_BROWSABLE) {
                    setTitle(res.getString(R.string.label_playing_queue))
                    setIconUri(iconRes(res, R.drawable.ic_queue_music_white_24dp))
                    setMediaId(MediaItemPath.pageQueue.mediaId)
                },
                mediaItem(FLAG_BROWSABLE) {
                    setTitle(res.getString(R.string.playlists))
                    setIconUri(iconRes(res, R.drawable.ic_description_white_24dp))
                    setMediaId(MediaItemPath.pagePlaylists.mediaId)
                },
                mediaItem(FLAG_BROWSABLE) {
                    setTitle(res.getString(R.string.favorites))
                    setIconUri(iconRes(res, R.drawable.ic_favorite_white_24dp))
                    setMediaId(MediaItemPath.pageFavorites.mediaId)
                },
                mediaItem(FLAG_BROWSABLE) {
                    setTitle(res.getString(R.string.my_top_tracks))
                    setIconUri(iconRes(res, R.drawable.ic_trending_up_white_24dp))
                    setMediaId(MediaItemPath.pageTopTracks.mediaId)
                },
                mediaItem(FLAG_BROWSABLE) {
                    setTitle(res.getString(R.string.last_added))
                    setIconUri(iconRes(res, R.drawable.ic_library_add_white_24dp))
                    setMediaId(MediaItemPath.pageLastAdded.mediaId)
                },
                mediaItem(FLAG_BROWSABLE) {
                    setTitle(res.getString(R.string.history))
                    setIconUri(iconRes(res, R.drawable.ic_access_time_white_24dp))
                    setMediaId(MediaItemPath.pageHistory.mediaId)
                },
                mediaItem(FLAG_BROWSABLE) {
                    setTitle(res.getString(R.string.genres))
                    setIconUri(iconRes(res, R.drawable.ic_bookmark_music_white_24dp))
                    setMediaId(MediaItemPath.pageGenres.mediaId)
                }
            )
        }

    }


    private object QueueProvider : AbsMediaItemProvider() {
        private val queueManager: QueueManager get() = GlobalContext.get().get()
        override suspend fun browser(context: Context): List<MediaItem> {
            val queue = queueManager.playingQueue
            return QueueSong.fromQueue(queue).map { it.toMediaItem() }
        }
    }


    private class QueueSongProvider(val index: Int) : AbsMediaItemProvider() {
        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.PlayAtRequest(index)
    }

    private object SongsProvider : AbsMediaItemProvider() {
        override suspend fun browser(context: Context): List<MediaItem> =
            Songs.all(context).map { it.toMediaItem() }

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(Songs.all(context), 0)
    }

    private class SongProvider(val songId: Long) : AbsMediaItemProvider() {
        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongRequest(Songs.id(context, songId))
    }

    private object AlbumsProvider : AbsMediaItemProvider() {
        override suspend fun browser(context: Context): List<MediaItem> =
            Albums.all(context).map { it.toMediaItem() }
    }

    private class AlbumProvider(val albumId: Long) : AbsMediaItemProvider() {
        private suspend fun fetch(context: Context): List<Song> = Songs.album(context, albumId)

        override suspend fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MediaItemPath.allAlbumSongs(albumId, false).mediaId,
                fetch(context).map { it.toMediaItem() }
            )

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(fetch(context), 0)
    }


    private object ArtistsProvider : AbsMediaItemProvider() {
        override suspend fun browser(context: Context): List<MediaItem> =
            Artists.all(context).map { it.toMediaItem() }
    }

    private class ArtistProvider(val artistId: Long) : AbsMediaItemProvider() {
        private suspend fun fetch(context: Context): List<Song> = Songs.artist(context, artistId)

        override suspend fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MediaItemPath.allArtistSongs(artistId, false).mediaId,
                fetch(context).map { it.toMediaItem() }
            )

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(fetch(context), 0)
    }


    private object PlaylistsProvider : AbsMediaItemProvider() {
        override suspend fun browser(context: Context): List<MediaItem> =
            PlaylistLoader.all(context).map { it.toMediaItem() }
    }

    private class PlaylistProvider(val playlistId: Long) : AbsMediaItemProvider() {
        private suspend fun fetch(context: Context) =
            PlaylistProcessors.reader(PlaylistLoader.id(context, playlistId)).allSongs(context)

        override suspend fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MediaItemPath.playlist(playlistId).mediaId,
                fetch(context).map { it.toMediaItem() }
            )

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(fetch(context), 0)
    }


    private object FavoriteSongsProvider : AbsMediaItemProvider() {
        private suspend fun fetch(context: Context): List<Song> = FavoritesStore.get().getAllSongs(context)

        override suspend fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MediaItemPath.allFavoritesSongs(false).mediaId,
                fetch(context).map { it.toMediaItem() }
            )

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(FavoritesStore.get().getAllSongs(context), 0)
    }

    private object TopTracksProvider : AbsMediaItemProvider() {
        private suspend fun fetch(context: Context): List<Song> = TopTracksLoader.get().tracks(context)

        override suspend fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MediaItemPath.allTopTracks(false).mediaId,
                fetch(context).map { it.toMediaItem() }
            )

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(TopTracksLoader.get().tracks(context), 0)
    }

    private object RecentAddedProvider : AbsMediaItemProvider() {

        private fun lastAddedCutoffTimeStamp(context: Context): Long =
            Setting(context).Composites[Keys.lastAddedCutoffTimeStamp].data / 1000

        private suspend fun fetch(context: Context): List<Song> =
            Songs.since(context, lastAddedCutoffTimeStamp(context))

        override suspend fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MediaItemPath.allLastAdded(false).mediaId,
                fetch(context).map { it.toMediaItem() }
            )

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(Songs.since(context, lastAddedCutoffTimeStamp(context)), 0)
    }


    private object RecentlyPlayedProvider : AbsMediaItemProvider() {
        private fun fetch(context: Context): List<Song> = RecentlyPlayedTracksLoader.get().tracks(context)

        override suspend fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MediaItemPath.allHistory(false).mediaId,
                fetch(context).map { it.toMediaItem() }
            )

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(RecentlyPlayedTracksLoader.get().tracks(context), 0)
    }



    private object GenresProvider : AbsMediaItemProvider() {
        override suspend fun browser(context: Context): List<MediaItem> =
            Genres.all(context).map { it.toMediaItem() }
    }

    private class GenreProvider(val genreId: Long) : AbsMediaItemProvider() {
        private suspend fun fetch(context: Context) = Songs.genres(context, genreId)

        override suspend fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MediaItemPath.genre(genreId).mediaId,
                fetch(context).map { it.toMediaItem() }
            )

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(fetch(context), 0)
    }

    @Suppress("unused")
    private const val TAG = "MediaItemProviders"


    /**
     * a MediaItem presenting errors
     */
    fun error(context: Context): MediaItem =
        MediaItem(
            MediaDescriptionCompat.Builder()
                .setTitle(context.getString(R.string.internal_error))
                .setMediaId(MediaItemPath.ROOT_PATH)
                .build(), FLAG_BROWSABLE
        )
}