/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.web.LastFmAction.Target
import util.phonograph.tagsources.lastfm.LastFmSearchResultResponse
import util.phonograph.tagsources.lastfm.LastFmSearchResults
import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LastFmQuery(
    context: Context,
    viewModel: WebSearchViewModel,
    albumQuery: String? = null,
    artistQuery: String? = null,
    trackQuery: String? = null,
    target: Target = Target.Album,
) : Query<LastFmQueryParameter, LastFmAction>(viewModel, Source.LastFm.name) {

    private val clientDelegate: LastFmClientDelegate = viewModel.clientDelegateLastFm(context)


    private val _queryParameter: MutableStateFlow<LastFmQueryParameter> =
        MutableStateFlow(LastFmQueryParameter(target, albumQuery, artistQuery, trackQuery))
    override val queryParameter get() = _queryParameter.asStateFlow()
    override fun updateQueryParameter(update: (LastFmQueryParameter) -> LastFmQueryParameter) {
        _queryParameter.update(update)
    }


    private val _result: MutableStateFlow<LastFmSearchResults?> = MutableStateFlow(null)
    val result get() = _result.asStateFlow()


    //region Query Implementations
    override fun query(context: Context, action: LastFmAction): Deferred<*> {
        val deferred = clientDelegate.request(context, action)
        viewModelScope.launch {
            when (val respond = deferred.await()) {
                is LastFmSearchResultResponse -> _result.emit(respond.results)
                else                          -> {}
            }
        }
        return deferred
    }
    //endregion


}