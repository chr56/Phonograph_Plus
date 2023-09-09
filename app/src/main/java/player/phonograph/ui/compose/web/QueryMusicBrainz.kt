/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.web.MusicBrainzAction.Target
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
    target: Target,
    query: String,
) : Query<MusicbrainzQueryParameter, MusicBrainzAction>(viewModel, Source.MusicBrainz.name) {


    private val _queryParameter: MutableStateFlow<MusicbrainzQueryParameter> =
        MutableStateFlow(MusicbrainzQueryParameter(target, query))
    override val queryParameter = _queryParameter.asStateFlow()
    override fun updateQueryParameter(update: (MusicbrainzQueryParameter) -> MusicbrainzQueryParameter) {
        _queryParameter.update(update)
    }

    private val _result: MutableStateFlow<MusicBrainzSearchResult?> = MutableStateFlow(null)
    val result get() = _result.asStateFlow()

    //region Query Implementations
    override fun query(context: Context, action: MusicBrainzAction): Deferred<*> {
        return musicBrainzQuery { service ->
            when (action) {
                is MusicBrainzAction.Search -> when (action.target) {
                    Target.ReleaseGroup -> searchMusicBrainzReleaseGroup(service, action.query)
                    Target.Release      -> searchMusicBrainzRelease(service, action.query)
                    Target.Artist       -> searchMusicBrainzArtists(service, action.query)
                    Target.Recording    -> searchMusicBrainzRecordings(service, action.query)
                }

                is MusicBrainzAction.View   -> when (action.target) {
                    Target.ReleaseGroup -> viewMusicBrainzReleaseGroup(service, action.mbid)
                    Target.Release      -> viewMusicBrainzRelease(service, action.mbid)
                    Target.Artist       -> viewMusicBrainzArtist(service, action.mbid)
                    Target.Recording    -> viewMusicBrainzRecording(service, action.mbid)
                }
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