/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.web.LastFmAction.Target
import util.phonograph.tagsources.lastfm.AlbumResult
import util.phonograph.tagsources.lastfm.ArtistResult
import util.phonograph.tagsources.lastfm.LastFMRestClient
import util.phonograph.tagsources.lastfm.LastFMService
import util.phonograph.tagsources.lastfm.LastFmAlbum
import util.phonograph.tagsources.lastfm.LastFmArtist
import util.phonograph.tagsources.lastfm.LastFmSearchResults
import util.phonograph.tagsources.lastfm.LastFmTrack
import util.phonograph.tagsources.lastfm.TrackResult
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LastFmQuery(
    context: Context,
    viewModel: WebSearchViewModel,
    albumQuery: String? = null,
    artistQuery: String? = null,
    trackQuery: String? = null,
    target: Target = Target.Album,
) : Query<LastFmQuery.QueryParameter, LastFmAction>(viewModel, Source.LastFm.name) {

    private val _queryParameter: MutableStateFlow<QueryParameter> =
        MutableStateFlow(QueryParameter(target, albumQuery, artistQuery, trackQuery))
    override val queryParameter get() = _queryParameter.asStateFlow()
    override fun updateQueryParameter(update: (QueryParameter) -> QueryParameter) {
        _queryParameter.update(update)
    }

    data class QueryParameter(
        val target: Target,
        val albumQuery: String?,
        val artistQuery: String?,
        val trackQuery: String?,
    ) : Parameter {
        fun check(): Boolean = when (target) {
            Target.Track  -> trackQuery != null
            Target.Artist -> artistQuery != null
            Target.Album  -> albumQuery != null
        }
    }

    fun searchAction(): LastFmAction.Search {
        with(queryParameter.value) {
            return when (target) {
                Target.Artist -> LastFmAction.Search.SearchArtist(artistQuery.orEmpty())
                Target.Album  -> LastFmAction.Search.SearchAlbum(albumQuery.orEmpty())
                Target.Track  -> LastFmAction.Search.SearchTrack(trackQuery.orEmpty(), artistQuery.orEmpty())
            }
        }
    }

    private val _result: MutableStateFlow<LastFmSearchResults?> = MutableStateFlow(null)
    val result get() = _result.asStateFlow()


    //region Query Implementations
    override fun query(context: Context, action: LastFmAction): Deferred<*> {
        return lastFmQuery { service ->
            when (action) {
                is LastFmAction.Search.SearchArtist -> searchArtist(service, action.name)
                is LastFmAction.Search.SearchAlbum  -> searchAlbum(service, action.name)
                is LastFmAction.Search.SearchTrack  -> searchTrack(service, action.name, action.artist)
                is LastFmAction.View.ViewArtist -> viewLastFMArtist(service, action.item)
                is LastFmAction.View.ViewAlbum  -> viewLastFMAlbum(service, action.item)
                is LastFmAction.View.ViewTrack  -> viewLastFMTrack(service, action.item)
            }
        }
    }

    private suspend fun searchArtist(service: LastFMService, name: String) {
        val call = service.searchArtist(name, 1)
        val searchResult = call.tryExecute()
        _result.emit(searchResult?.results)
    }


    private suspend fun searchAlbum(service: LastFMService, name: String) {
        val call = service.searchAlbum(name, 1)
        val searchResult = call.tryExecute()
        _result.emit(searchResult?.results)
    }

    private suspend fun searchTrack(service: LastFMService, name: String, artist: String?) {
        val call = service.searchTrack(name, artist, 1)
        val searchResult = call.tryExecute()
        _result.emit(searchResult?.results)
    }


    private suspend fun viewLastFMAlbum(service: LastFMService, album: AlbumResult.Album): LastFmAlbum? {
        val call = service.getAlbumInfo(album.name, album.artist, null)
        return call.tryExecute()?.album
    }

    private suspend fun viewLastFMArtist(service: LastFMService, artist: ArtistResult.Artist): LastFmArtist? {
        val call = service.getArtistInfo(artist.name, null, null)
        return call.tryExecute()?.artist
    }

    private suspend fun viewLastFMTrack(service: LastFMService, track: TrackResult.Track): LastFmTrack? {
        val call = service.getTrackInfo(track.name, track.artist, null)
        return call.tryExecute()?.track
    }

    private fun <T> lastFmQuery(
        block: suspend CoroutineScope.(LastFMService) -> T,
    ): Deferred<T> {
        return viewModelScope.async(Dispatchers.IO) {
            val service = lastFMRestClient.apiService
            block.invoke(this, service)
        }.also { lastFmQueryJob = it }
    }


    private val lastFMRestClient: LastFMRestClient = LastFMRestClient(context)
    private var lastFmQueryJob: Job? = null
    //endregion


}