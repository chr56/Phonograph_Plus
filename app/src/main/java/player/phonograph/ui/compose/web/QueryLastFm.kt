/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import util.phonograph.tagsources.lastfm.AlbumResult
import util.phonograph.tagsources.lastfm.ArtistResult
import util.phonograph.tagsources.lastfm.LastFMRestClient
import util.phonograph.tagsources.lastfm.LastFMService
import util.phonograph.tagsources.lastfm.LastFmAlbum
import util.phonograph.tagsources.lastfm.LastFmArtist
import util.phonograph.tagsources.lastfm.LastFmSearchResultItem
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
    releaseQuery: String? = null,
    artistQuery: String? = null,
    trackQuery: String? = null,
    target: Target = Target.Release,
) : Query<LastFmQuery.QueryParameter, LastFmQuery.QueryAction>(viewModel, "Last.fm") {

    enum class Target {
        Artist,
        Release,
        Track,
        ;
    }

    sealed class QueryAction : Action {
        data class SearchArtist(val name: String) : QueryAction()
        data class SearchRelease(val name: String) : QueryAction()
        data class SearchTrack(val name: String, val artist: String?) : QueryAction()
        data class ViewArtist(val item: ArtistResult.Artist) : QueryAction()
        data class ViewRelease(val item: AlbumResult.Album) : QueryAction()
        data class ViewTrack(val item: TrackResult.Track) : QueryAction()
    }

    private val _queryParameter: MutableStateFlow<QueryParameter> =
        MutableStateFlow(QueryParameter(target, releaseQuery, artistQuery, trackQuery))
    override val queryParameter get() = _queryParameter.asStateFlow()
    override fun updateQueryParameter(update: (QueryParameter) -> QueryParameter) {
        _queryParameter.update(update)
    }

    data class QueryParameter(
        val target: Target,
        val releaseQuery: String?,
        val artistQuery: String?,
        val trackQuery: String?,
    ) : Parameter {
        fun check(): Boolean = when (target) {
            Target.Track   -> trackQuery != null
            Target.Artist  -> artistQuery != null
            Target.Release -> releaseQuery != null
        }
    }

    fun searchAction(): QueryAction {
        with(queryParameter.value) {
            return when (target) {
                Target.Artist  -> QueryAction.SearchArtist(artistQuery.orEmpty())
                Target.Release -> QueryAction.SearchRelease(releaseQuery.orEmpty())
                Target.Track   -> QueryAction.SearchTrack(trackQuery.orEmpty(), artistQuery.orEmpty())
            }
        }
    }

    fun viewAction(selected: LastFmSearchResultItem): QueryAction {
        return when (selected) {
            is AlbumResult.Album   -> QueryAction.ViewRelease(selected)
            is ArtistResult.Artist -> QueryAction.ViewArtist(selected)
            is TrackResult.Track   -> QueryAction.ViewTrack(selected)
        }
    }

    private val _result: MutableStateFlow<LastFmSearchResults?> = MutableStateFlow(null)
    val result get() = _result.asStateFlow()


    //region Query Implementations
    override fun query(context: Context, action: QueryAction): Deferred<*> {
        return lastFmQuery { service ->
            when (action) {
                is QueryAction.SearchArtist  -> searchArtist(service, action.name)
                is QueryAction.SearchRelease -> searchAlbum(service, action.name)
                is QueryAction.SearchTrack   -> searchTrack(service, action.name, action.artist)
                is QueryAction.ViewArtist    -> viewLastFMArtist(service, action.item)
                is QueryAction.ViewRelease   -> viewLastFMAlbum(service, action.item)
                is QueryAction.ViewTrack     -> viewLastFMTrack(service, action.item)
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