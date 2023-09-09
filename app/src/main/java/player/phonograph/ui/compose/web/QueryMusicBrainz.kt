/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.web.MusicBrainzAction.Target
import util.phonograph.tagsources.musicbrainz.MusicBrainzSearchResult
import util.phonograph.tagsources.musicbrainz.MusicBrainzSearchResultArtists
import util.phonograph.tagsources.musicbrainz.MusicBrainzSearchResultRecording
import util.phonograph.tagsources.musicbrainz.MusicBrainzSearchResultReleases
import util.phonograph.tagsources.musicbrainz.MusicBrainzSearchResultReleasesGroup
import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MusicBrainzQuery(
    context: Context,
    viewModel: WebSearchViewModel,
    target: Target,
    query: String,
) : Query<MusicbrainzQueryParameter, MusicBrainzAction>(viewModel, Source.MusicBrainz.name) {

    private val clientDelegate: MusicBrainzClientDelegate = viewModel.clientDelegateMusicBrainz(context)

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
        val deferred = clientDelegate.request(context, action)
        viewModelScope.launch {
            when (val respond = deferred.await()) {
                is MusicBrainzSearchResultArtists       -> _result.emit(respond)
                is MusicBrainzSearchResultRecording     -> _result.emit(respond)
                is MusicBrainzSearchResultReleases      -> _result.emit(respond)
                is MusicBrainzSearchResultReleasesGroup -> _result.emit(respond)
                else                                    -> _result.emit(null)
            }
        }
        return deferred
    }
    //endregion

}