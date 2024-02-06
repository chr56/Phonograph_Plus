/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.lastfm

import util.phonograph.tagsources.AbsClientDelegate
import util.phonograph.tagsources.lastfm.LastFmAction.Search
import util.phonograph.tagsources.lastfm.LastFmAction.View
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class LastFmClientDelegate(
    context: Context,
    userAgent: String,
    exceptionHandler: ExceptionHandler,
    val scope: CoroutineScope,
) : AbsClientDelegate<LastFmAction, LastFmResponse>(exceptionHandler) {

    private val musicBrainzRestClient: LastFMRestClient = LastFMRestClient(context, userAgent)

    override fun request(context: Context, action: LastFmAction): Deferred<LastFmResponse?> {
        return scope.async(Dispatchers.IO) {
            val api = musicBrainzRestClient.apiService
            when (action) {
                is Search          -> when (action.target) {
                    LastFmAction.Target.Artist -> api.searchArtist(action.artist, 1).process()
                    LastFmAction.Target.Album  -> api.searchAlbum(action.album, 1).process()
                    LastFmAction.Target.Track  -> api.searchTrack(action.track, action.artist, 1).process()
                }

                is View.ViewAlbum  -> api.getAlbumInfo(action.item.name, action.item.artist, action.language).process()
                is View.ViewArtist -> api.getArtistInfo(action.item.name, action.language, null).process()
                is View.ViewTrack  -> api.getTrackInfo(action.item.name, action.item.artist, action.language).process()
            }
        }
    }

}