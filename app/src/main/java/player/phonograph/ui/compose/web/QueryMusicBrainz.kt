/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import util.phonograph.tagsources.musicbrainz.MusicBrainzModel
import util.phonograph.tagsources.musicbrainz.MusicBrainzRestClient
import util.phonograph.tagsources.musicbrainz.MusicBrainzService
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MusicBrainzQuery(
    context: Context,
    viewModel: WebSearchViewModel,
) : Query<MusicBrainzQuery.QueryParameter, MusicBrainzQuery.QueryAction>(viewModel, "Musicbrainz") {

    enum class Target {
        ReleaseGroup,
        Release,
        Artist,
        Recording,
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

    private val _detail: MutableStateFlow<MusicBrainzModel?> = MutableStateFlow(null)
    val detail get() = _detail.asStateFlow()


    //region Query Implementations
    override fun query(context: Context, action: QueryAction) {
        musicBrainzQuery { service ->
            when (action) {
                is QueryAction.ViewReleaseGroup -> viewMusicBrainzReleaseGroup(service, action.mbid)
                is QueryAction.ViewRelease      -> viewMusicBrainzRelease(service, action.mbid)
                is QueryAction.ViewArtist       -> viewMusicBrainzArtist(service, action.mbid)
                is QueryAction.ViewRecording    -> viewMusicBrainzRecording(service, action.mbid)
            }
        }
    }

    private suspend fun viewMusicBrainzReleaseGroup(service: MusicBrainzService, mbid: String) {
        val call = service.getReleaseGroup(mbid)
        val response = call.tryExecute()
        _detail.emit(response)
    }

    private suspend fun viewMusicBrainzRelease(service: MusicBrainzService, mbid: String) {
        val call = service.getRelease(mbid)
        val response = call.tryExecute()
        _detail.emit(response)
    }

    private suspend fun viewMusicBrainzArtist(service: MusicBrainzService, mbid: String) {
        val call = service.getArtist(mbid)
        val response = call.tryExecute()
        _detail.emit(response)
    }

    private suspend fun viewMusicBrainzRecording(service: MusicBrainzService, mbid: String) {
        val call = service.getRecording(mbid)
        val response = call.tryExecute()
        _detail.emit(response)
    }

    private fun musicBrainzQuery(
        block: suspend CoroutineScope.(MusicBrainzService) -> Unit,
    ) {
        musicBrainzQueryJob?.cancel()
        musicBrainzQueryJob = viewModelScope.launch(Dispatchers.IO) {
            val service = musicBrainzRestClient.apiService
            block.invoke(this, service)
        }
    }
    //endregion


    private val musicBrainzRestClient: MusicBrainzRestClient = MusicBrainzRestClient(context)
    private var musicBrainzQueryJob: Job? = null

}