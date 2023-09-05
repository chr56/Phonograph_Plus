/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import util.phonograph.tagsources.musicbrainz.MusicBrainzArtist
import util.phonograph.tagsources.musicbrainz.MusicBrainzRecording
import util.phonograph.tagsources.musicbrainz.MusicBrainzRelease
import util.phonograph.tagsources.musicbrainz.MusicBrainzReleaseGroup
import util.phonograph.tagsources.musicbrainz.MusicBrainzRestClient
import util.phonograph.tagsources.musicbrainz.MusicBrainzService
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

    enum class Target(val urlName: String) {
        ReleaseGroup("release-group"),
        Release("release"),
        Artist("artist"),
        Recording("recording"),
        ;
    }

    private val _queryParameter: MutableStateFlow<QueryParameter> = MutableStateFlow(QueryParameter())
    override val queryParameter = _queryParameter.asStateFlow()
    override fun updateQueryParameter(update: (QueryParameter) -> QueryParameter) {
        _queryParameter.update(update)
    }

    class QueryParameter() : Parameter

    sealed class QueryAction : Action {
        data class ViewArtist(val mbid: String) : QueryAction()
        data class ViewRelease(val mbid: String) : QueryAction()
        data class ViewReleaseGroup(val mbid: String) : QueryAction()
        data class ViewRecording(val mbid: String) : QueryAction()
    }

    //region Query Implementations
    override fun query(context: Context, action: QueryAction): Deferred<*> {
        return musicBrainzQuery { service ->
            when (action) {
                is QueryAction.ViewReleaseGroup -> viewMusicBrainzReleaseGroup(service, action.mbid)
                is QueryAction.ViewRelease      -> viewMusicBrainzRelease(service, action.mbid)
                is QueryAction.ViewArtist       -> viewMusicBrainzArtist(service, action.mbid)
                is QueryAction.ViewRecording    -> viewMusicBrainzRecording(service, action.mbid)
            }
        }
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