/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import util.phonograph.tagsources.lastfm.AlbumResult
import util.phonograph.tagsources.lastfm.ArtistResult
import util.phonograph.tagsources.lastfm.LastFMRestClient
import util.phonograph.tagsources.lastfm.LastFMService
import util.phonograph.tagsources.lastfm.LastFmSearchResults
import util.phonograph.tagsources.lastfm.TrackResult
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LastFmQuery(
    context: Context,
    releaseQuery: String? = null,
    artistQuery: String? = null,
    trackQuery: String? = null,
    target: Target = Target.Release,
) : Query("Last.fm") {

    enum class Target {
        Artist,
        Release,
        Track,
        ;
    }

    val target: MutableStateFlow<Target> = MutableStateFlow(target)

    val releaseQuery: MutableStateFlow<String?> = MutableStateFlow(releaseQuery)
    val artistQuery: MutableStateFlow<String?> = MutableStateFlow(artistQuery)
    val trackQuery: MutableStateFlow<String?> = MutableStateFlow(trackQuery)

    fun action(): QueryAction {
        return when (target.value) {
            Target.Artist  -> QueryAction.Artist(artistQuery.value.orEmpty())
            Target.Release -> QueryAction.Release(releaseQuery.value.orEmpty())
            Target.Track   -> QueryAction.Track(trackQuery.value.orEmpty(), artistQuery.value.orEmpty())
        }
    }

    sealed class QueryAction : Action {
        data class Artist(val name: String) : QueryAction()
        data class Release(val name: String) : QueryAction()
        data class Track(val name: String, val artist: String?) : QueryAction()
    }

    private val _result: MutableStateFlow<LastFmSearchResults?> = MutableStateFlow(null)
    val result get() = _result.asStateFlow()


    private val _detail: MutableStateFlow<Any?> = MutableStateFlow(null)
    val detail get() = _detail.asStateFlow()

    //region Query Implementations
    override fun search(context: Context, action: Action) {
        lastFmQuery(scope) { service ->
            val call = when (action) {
                is QueryAction.Artist  -> service.searchArtist(action.name, 1)
                is QueryAction.Release -> service.searchAlbum(action.name, 1)
                is QueryAction.Track   -> service.searchTrack(action.name, action.artist, 1)
                else                   -> throw IllegalStateException()
            }
            val searchResult = execute(call)
            if (searchResult != null) {
                _result.emit(searchResult.results)
            }
        }
    }

    override fun view(context: Context, item: Any) {
        scope.launch(Dispatchers.IO) {
            when (item) {
                is AlbumResult.Album   -> queryLastFMAlbum(context, item)
                is ArtistResult.Artist -> queryLastFMArtist(context, item)
                is TrackResult.Track   -> queryLastFMTrack(context, item)
            }
        }
    }

    private fun queryLastFMAlbum(context: Context, album: AlbumResult.Album) {
        lastFmQuery(scope) { service ->
            val call = service.getAlbumInfo(album.name, album.artist, null)
            val response = execute(call)
            _detail.emit(response?.album)
        }
    }

    private fun queryLastFMArtist(context: Context, artist: ArtistResult.Artist) {
        lastFmQuery(scope) { service ->
            val call = service.getArtistInfo(artist.name, null, null)
            val response = execute(call)
            _detail.emit(response?.artist)
        }
    }

    private fun queryLastFMTrack(context: Context, track: TrackResult.Track) {
        lastFmQuery(scope) { service ->
            val call = service.getTrackInfo(track.name, track.artist, null)
            val response = execute(call)
            _detail.emit(response?.track)
        }
    }

    private fun lastFmQuery(
        scope: CoroutineScope,
        block: suspend CoroutineScope.(LastFMService) -> Unit,
    ) {
        lastFmQueryJob?.cancel()
        lastFmQueryJob = scope.launch(Dispatchers.IO) {
            val service = lastFMRestClient?.apiService
            if (service != null) {
                block.invoke(this, service)
            }
        }
    }


    private val lastFMRestClient: LastFMRestClient = LastFMRestClient(context)
    private var lastFmQueryJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    //endregion


    companion object {
        fun from(context: Context, artist: Artist): LastFmQuery =
            LastFmQuery(
                context,
                artistQuery = artist.name,
                target = Target.Artist
            )

        fun from(context: Context, album: Album): LastFmQuery =
            LastFmQuery(
                context,
                releaseQuery = album.title,
                artistQuery = album.artistName,
                target = Target.Release
            )

        fun from(context: Context, song: Song): LastFmQuery =
            LastFmQuery(
                context,
                releaseQuery = song.albumName,
                artistQuery = song.artistName,
                trackQuery = song.title,
                target = Target.Track
            )
    }
}