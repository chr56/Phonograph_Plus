/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.foundation.mediastore.mediaStoreUriAlbumArt
import player.phonograph.foundation.mediastore.mediaStoreUriSongExternal
import player.phonograph.mechanism.playlist.PlaylistSongsActions
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.PlayRequest
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.sort.SortMode
import player.phonograph.repo.database.domain.DynamicTracks
import player.phonograph.repo.loader.Albums
import player.phonograph.repo.loader.Artists
import player.phonograph.repo.loader.FavoriteTracks
import player.phonograph.repo.loader.Genres
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.service.queue.QueueManager
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import androidx.annotation.DrawableRes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri

object MediaItemProviders {

    /**
     * Present a MediaItem
     */
    interface MediaItemProvider {
        suspend fun browser(context: Context): List<MediaItem>
        suspend fun play(context: Context): PlayRequest
    }


    fun of(path: String): MediaItemProvider = try {
        parse(path)
    } catch (_: NumberFormatException) {
        EmptyProvider
    }


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
            mediaItem(false, path) {
                setTitle(resources.getString(R.string.action_play_all))
                setArtworkUri(iconRes(resources, R.drawable.ic_play_arrow_white_24dp))
            }

        /**
         * create a MediaItem
         */
        protected fun mediaItem(browsable: Boolean, mediaId: String, block: MediaMetadata.Builder.() -> Unit): MediaItem =
            MediaItem.Builder().setMediaId(mediaId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(browsable)
                        .setIsPlayable(!browsable)
                        .apply { if (browsable) setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED) }
                        .apply(block).build()
                ).build()

        protected fun iconRes(res: Resources, @DrawableRes resourceId: Int): Uri =
            Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(res.getResourcePackageName(resourceId))
                .appendPath(res.getResourceTypeName(resourceId))
                .appendPath(res.getResourceEntryName(resourceId))
                .build()

        protected fun QueueSong.toMediaItem(): MediaItem =
            mediaItem(false, MediaItemPath.queueSong(index).mediaId) {
                setTitle(song.title)
                setSubtitle(song.albumName)
                setDescription(song.artistName)
            }

        protected fun Album.toMediaItem(): MediaItem =
            mediaItem(true, MediaItemPath.album(id).mediaId) {
                setTitle(title)
                setSubtitle(artistName)
                setDescription(artistName)
                setArtworkUri(mediaStoreUriAlbumArt(id))
            }

        protected fun Artist.toMediaItem(): MediaItem =
            mediaItem(true, MediaItemPath.artist(id).mediaId) {
                setTitle(name)
            }

        protected fun Playlist.toMediaItem(): MediaItem =
            mediaItem(true, MediaItemPath.playlist(mediaStoreId()!!).mediaId) {
                setTitle(name)
            }

        protected fun Genre.toMediaItem(): MediaItem =
            mediaItem(true, MediaItemPath.genre(id).mediaId) {
                setTitle(name)
                setSubtitle(songCount.toString())
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
                mediaItem(true, MediaItemPath.pageSongs.mediaId) {
                    setTitle(res.getString(R.string.label_songs))
                    setArtworkUri(iconRes(res, R.drawable.ic_music_note_white_24dp))
                },
                mediaItem(true, MediaItemPath.pageAlbums.mediaId) {
                    setTitle(res.getString(R.string.label_albums))
                    setArtworkUri(iconRes(res, R.drawable.ic_album_white_24dp))
                },
                mediaItem(true, MediaItemPath.pageArtist.mediaId) {
                    setTitle(res.getString(R.string.label_artists))
                    setArtworkUri(iconRes(res, R.drawable.ic_person_white_24dp))
                },
                mediaItem(true, MediaItemPath.pageQueue.mediaId) {
                    setTitle(res.getString(R.string.label_playing_queue))
                    setArtworkUri(iconRes(res, R.drawable.ic_queue_music_white_24dp))
                },
                mediaItem(true, MediaItemPath.pagePlaylists.mediaId) {
                    setTitle(res.getString(R.string.label_playlists))
                    setArtworkUri(iconRes(res, R.drawable.ic_description_white_24dp))
                },
                mediaItem(true, MediaItemPath.pageFavorites.mediaId) {
                    setTitle(res.getString(R.string.playlist_favorites))
                    setArtworkUri(iconRes(res, R.drawable.ic_favorite_white_24dp))
                },
                mediaItem(true, MediaItemPath.pageTopTracks.mediaId) {
                    setTitle(res.getString(R.string.playlist_my_top_tracks))
                    setArtworkUri(iconRes(res, R.drawable.ic_trending_up_white_24dp))
                },
                mediaItem(true, MediaItemPath.pageLastAdded.mediaId) {
                    setTitle(res.getString(R.string.playlist_last_added))
                    setArtworkUri(iconRes(res, R.drawable.ic_library_add_white_24dp))
                },
                mediaItem(true, MediaItemPath.pageHistory.mediaId) {
                    setTitle(res.getString(R.string.playlist_history))
                    setArtworkUri(iconRes(res, R.drawable.ic_access_time_white_24dp))
                },
                mediaItem(true, MediaItemPath.pageGenres.mediaId) {
                    setTitle(res.getString(R.string.label_genres))
                    setArtworkUri(iconRes(res, R.drawable.ic_bookmark_music_white_24dp))
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
            Songs.all(context, sortMode(context)).map { it.toMediaItem() }

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(Songs.all(context, sortMode(context)), 0)

        private suspend fun sortMode(context: Context): SortMode = Settings(context)[Keys.songSortMode].read()
    }

    private class SongProvider(val songId: Long) : AbsMediaItemProvider() {
        override suspend fun play(context: Context): PlayRequest {
            val song = Songs.id(context, songId)
            return if (song != null) PlayRequest.SongRequest(song) else PlayRequest.EmptyRequest
        }
    }

    private object AlbumsProvider : AbsMediaItemProvider() {
        override suspend fun browser(context: Context): List<MediaItem> =
            Albums.all(context, Settings(context)[Keys.albumSortMode].read()).map { it.toMediaItem() }
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
            Artists.all(context, Settings(context)[Keys.artistSortMode].read()).map { it.toMediaItem() }
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
            MediaStorePlaylists.all(context).map { it.toMediaItem() }
    }

    private class PlaylistProvider(val playlistId: Long) : AbsMediaItemProvider() {
        private suspend fun fetch(context: Context) =
            PlaylistSongsActions.reader(MediaStorePlaylists.id(context, playlistId)!!).allSongs(context)

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
        private suspend fun fetch(context: Context): List<Song> = FavoriteTracks.all(context)

        override suspend fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MediaItemPath.allFavoritesSongs(false).mediaId,
                fetch(context).map { it.toMediaItem() }
            )

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(FavoriteTracks.all(context), 0)
    }

    private object TopTracksProvider : AbsMediaItemProvider() {
        private suspend fun fetch(context: Context): List<Song> = DynamicTracks.TopTracks.all(context)

        override suspend fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MediaItemPath.allTopTracks(false).mediaId,
                fetch(context).map { it.toMediaItem() }
            )

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(DynamicTracks.TopTracks.all(context), 0)
    }

    private object RecentAddedProvider : AbsMediaItemProvider() {

        private fun lastAddedCutoffTimeStamp(context: Context): Long =
            Settings(context)[Keys.lastAddedCutoffTimeStamp].data / 1000

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
        private suspend fun fetch(context: Context): List<Song> = DynamicTracks.RecentTracks.all(context)

        override suspend fun browser(context: Context): List<MediaItem> =
            withPlayAllItems(
                context.resources,
                MediaItemPath.allHistory(false).mediaId,
                fetch(context).map { it.toMediaItem() }
            )

        override suspend fun play(context: Context): PlayRequest =
            PlayRequest.SongsRequest(DynamicTracks.RecentTracks.all(context), 0)
    }



    private object GenresProvider : AbsMediaItemProvider() {
        override suspend fun browser(context: Context): List<MediaItem> =
            Genres.all(context, Settings(context)[Keys.genreSortMode].read()).map { it.toMediaItem() }
    }

    private class GenreProvider(val genreId: Long) : AbsMediaItemProvider() {
        private suspend fun fetch(context: Context) = Genres.songs(context, genreId)

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
        MediaItem.Builder().setMediaId(MediaItemPath.ROOT_PATH)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(context.getString(R.string.title_internal_error))
                    .setIsBrowsable(true).setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            ).build()
}

fun Song.toMediaMetadata(): MediaMetadata = MediaMetadata.Builder()
    .setTitle(title)
    .setArtist(artistName)
    .setAlbumTitle(albumName)
    .setAlbumArtist(artistName)
    .setRecordingYear(year.takeIf { it > 0 })
    .setDurationMs(duration.coerceAtLeast(0))
    .setArtworkUri(mediaStoreUriAlbumArt(albumId))
    .setIsBrowsable(false).setIsPlayable(true)
    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
    .build()

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(MediaItemPath.song(id).mediaId)
    .setUri(mediaStoreUriSongExternal(id))
    .setTag(this)
    .setMediaMetadata(toMediaMetadata())
    .build()