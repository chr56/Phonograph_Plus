/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import util.phonograph.tagsources.musicbrainz.MusicBrainzArtist
import util.phonograph.tagsources.musicbrainz.MusicBrainzRecording
import util.phonograph.tagsources.musicbrainz.MusicBrainzRelease
import util.phonograph.tagsources.musicbrainz.MusicBrainzReleaseGroup
import util.phonograph.tagsources.musicbrainz.MusicBrainzRestClient
import util.phonograph.tagsources.musicbrainz.MusicBrainzSearchResult
import util.phonograph.tagsources.musicbrainz.MusicBrainzSearchResultArtists
import util.phonograph.tagsources.musicbrainz.MusicBrainzSearchResultRecording
import util.phonograph.tagsources.musicbrainz.MusicBrainzSearchResultReleases
import util.phonograph.tagsources.musicbrainz.MusicBrainzSearchResultReleasesGroup
import util.phonograph.tagsources.musicbrainz.MusicBrainzService
import androidx.annotation.Keep
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MusicBrainzQuery(
    context: Context,
    viewModel: WebSearchViewModel,
) : Query<MusicBrainzQuery.QueryParameter, MusicBrainzQuery.QueryAction>(viewModel, "Musicbrainz") {

    @Keep
    enum class Target(val displayName: String, val urlName: String) {
        ReleaseGroup("Release Group", "release-group"),
        Release("Release", "release"),
        Artist("Artist", "artist"),
        Recording("Recording", "recording"),
        ;

        fun link(mbid: String): String = "https://musicbrainz.org/${urlName}/$mbid"
    }

    private val _queryParameter: MutableStateFlow<QueryParameter> = MutableStateFlow(QueryParameter(Target.Release, ""))
    override val queryParameter = _queryParameter.asStateFlow()
    override fun updateQueryParameter(update: (QueryParameter) -> QueryParameter) {
        _queryParameter.update(update)
    }

    data class QueryParameter(var target: Target, var query: String) : Parameter {
        fun searchAction(): QueryAction = when (target) {
            Target.ReleaseGroup -> QueryAction.SearchReleaseGroup(query)
            Target.Release      -> QueryAction.SearchRelease(query)
            Target.Artist       -> QueryAction.SearchArtist(query)
            Target.Recording    -> QueryAction.SearchRecording(query)
        }
    }

    sealed class QueryAction : Action {
        data class SearchArtist(val query: String) : QueryAction()
        data class SearchRelease(val query: String) : QueryAction()
        data class SearchReleaseGroup(val query: String) : QueryAction()
        data class SearchRecording(val query: String) : QueryAction()
        data class ViewArtist(val mbid: String) : QueryAction()
        data class ViewRelease(val mbid: String) : QueryAction()
        data class ViewReleaseGroup(val mbid: String) : QueryAction()
        data class ViewRecording(val mbid: String) : QueryAction()
    }


    private val _result: MutableStateFlow<MusicBrainzSearchResult?> = MutableStateFlow(null)
    val result get() = _result.asStateFlow()

    //region Query Implementations
    override fun query(context: Context, action: QueryAction): Deferred<*> {
        return musicBrainzQuery { service ->
            when (action) {
                is QueryAction.ViewReleaseGroup   -> viewMusicBrainzReleaseGroup(service, action.mbid)
                is QueryAction.ViewRelease        -> viewMusicBrainzRelease(service, action.mbid)
                is QueryAction.ViewArtist         -> viewMusicBrainzArtist(service, action.mbid)
                is QueryAction.ViewRecording      -> viewMusicBrainzRecording(service, action.mbid)
                is QueryAction.SearchReleaseGroup -> searchMusicBrainzReleaseGroup(service, action.query)
                is QueryAction.SearchRelease      -> searchMusicBrainzRelease(service, action.query)
                is QueryAction.SearchArtist       -> searchMusicBrainzArtists(service, action.query)
                is QueryAction.SearchRecording    -> searchMusicBrainzRecordings(service, action.query)
            }
        }
    }


    private suspend fun searchMusicBrainzReleaseGroup(
        service: MusicBrainzService,
        query: String,
    ): MusicBrainzSearchResultReleasesGroup? {
        val call = service.searchReleaseGroup(query, 0)
        return call.tryExecute().also { _result.tryEmit(it) }
    }

    private suspend fun searchMusicBrainzRelease(
        service: MusicBrainzService,
        query: String,
    ): MusicBrainzSearchResultReleases? {
        val call = service.searchRelease(query, 0)
        return call.tryExecute().also { _result.tryEmit(it) }
    }

    private suspend fun searchMusicBrainzArtists(
        service: MusicBrainzService,
        query: String,
    ): MusicBrainzSearchResultArtists? {
        val call = service.searchArtist(query, 0)
        return call.tryExecute().also { _result.tryEmit(it) }
    }

    private suspend fun searchMusicBrainzRecordings(
        service: MusicBrainzService,
        query: String,
    ): MusicBrainzSearchResultRecording? {
        val call = service.searchRecording(query, 0)
        return call.tryExecute().also { _result.tryEmit(it) }
    }

    private suspend fun viewMusicBrainzReleaseGroup(
        service: MusicBrainzService,
        mbid: String,
    ): MusicBrainzReleaseGroup? {
        val call = service.getReleaseGroup(mbid)
        return call.tryExecute()
    }

    private suspend fun viewMusicBrainzRelease(service: MusicBrainzService, mbid: String): MusicBrainzRelease? {
        val call = service.getRelease(mbid)
        return call.tryExecute()
    }

    private suspend fun viewMusicBrainzArtist(service: MusicBrainzService, mbid: String): MusicBrainzArtist? {
        val call = service.getArtist(mbid)
        return call.tryExecute()
    }

    private suspend fun viewMusicBrainzRecording(service: MusicBrainzService, mbid: String): MusicBrainzRecording? {
        val call = service.getRecording(mbid)
        return call.tryExecute()
    }

    private fun <T> musicBrainzQuery(
        block: suspend CoroutineScope.(MusicBrainzService) -> T,
    ): Deferred<T> {
        return viewModelScope.async(Dispatchers.IO) {
            val service = musicBrainzRestClient.apiService
            block.invoke(this, service)
        }
    }

    private val musicBrainzRestClient: MusicBrainzRestClient = MusicBrainzRestClient(context)
    private var musicBrainzQueryJob: Job? = null
    //endregion

}