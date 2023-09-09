/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.ui.compose.web.LastFmAction.Search
import player.phonograph.ui.compose.web.LastFmAction.View
import util.phonograph.tagsources.lastfm.LastFMRestClient
import util.phonograph.tagsources.lastfm.LastFmResponse
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class LastFmClientDelegate(
    context: Context,
    val scope: CoroutineScope,
) : ClientDelegate<LastFmAction, LastFmResponse>() {

    private val musicBrainzRestClient: LastFMRestClient = LastFMRestClient(context)

    override fun request(context: Context, action: LastFmAction): Deferred<LastFmResponse?> {
        return scope.async(Dispatchers.IO) {
            val api = musicBrainzRestClient.apiService
            when (action) {
                is Search.SearchAlbum  -> api.searchAlbum(action.name, 1).process()
                is Search.SearchArtist -> api.searchArtist(action.name, 1).process()
                is Search.SearchTrack  -> api.searchTrack(action.name, action.artist, 1).process()
                is View.ViewAlbum      -> api.getAlbumInfo(action.item.name, action.item.artist, null).process()
                is View.ViewArtist     -> api.getArtistInfo(action.item.name, null, null).process()
                is View.ViewTrack      -> api.getTrackInfo(action.item.name, action.item.artist, null).process()
            }
        }
    }

}