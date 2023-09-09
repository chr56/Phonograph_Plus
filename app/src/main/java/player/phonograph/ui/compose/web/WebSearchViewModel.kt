/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.ui.compose.base.Navigator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context

class WebSearchViewModel : ViewModel() {

    val navigator = Navigator<Page>(PageHome)

    private var clientDelegateLastFm: LastFmClientDelegate? = null
    fun clientDelegateLastFm(context: Context): LastFmClientDelegate {
        return if (clientDelegateLastFm != null) {
            clientDelegateLastFm!!
        } else {
            LastFmClientDelegate(context, viewModelScope).also { clientDelegateLastFm = it }
        }
    }

    private var clientDelegateMusicBrainz: MusicBrainzClientDelegate? = null
    fun clientDelegateMusicBrainz(context: Context): MusicBrainzClientDelegate {
        return if (clientDelegateMusicBrainz != null) {
            clientDelegateMusicBrainz!!
        } else {
            MusicBrainzClientDelegate(context, viewModelScope).also { clientDelegateMusicBrainz = it }
        }
    }

    val queryFactory = QueryFactory()

    @Suppress("DeferredResultUnused")
    inner class QueryFactory {

        fun lastFmQuery(context: Context): LastFmQuery = LastFmQuery(context, this@WebSearchViewModel)

        fun lastFmQuery(context: Context, artist: Artist): LastFmQuery =
            LastFmQuery(
                context, this@WebSearchViewModel,
                artistQuery = artist.name,
                target = LastFmAction.Target.Artist
            )

        fun lastFmQuery(context: Context, album: Album): LastFmQuery =
            LastFmQuery(
                context, this@WebSearchViewModel,
                albumQuery = album.title,
                artistQuery = album.artistName,
                target = LastFmAction.Target.Album
            )

        fun lastFmQuery(context: Context, song: Song): LastFmQuery =
            LastFmQuery(
                context, this@WebSearchViewModel,
                albumQuery = song.albumName,
                artistQuery = song.artistName,
                trackQuery = song.title,
                target = LastFmAction.Target.Track
            )

        fun lastFmQuery(context: Context, action: LastFmAction.Search): LastFmQuery = when (action) {
            is LastFmAction.Search.SearchArtist -> LastFmQuery(
                context, this@WebSearchViewModel,
                artistQuery = action.name,
                target = LastFmAction.Target.Artist
            )

            is LastFmAction.Search.SearchAlbum  -> LastFmQuery(
                context, this@WebSearchViewModel,
                albumQuery = action.name,
                target = LastFmAction.Target.Album
            )

            is LastFmAction.Search.SearchTrack  -> LastFmQuery(
                context, this@WebSearchViewModel,
                trackQuery = action.name,
                artistQuery = action.artist,
                target = LastFmAction.Target.Track
            )
        }


        fun musicBrainzQuery(context: Context): MusicBrainzQuery =
            MusicBrainzQuery(context, this@WebSearchViewModel, MusicBrainzAction.Target.Release, "")

        fun musicBrainzQuery(context: Context, target: MusicBrainzAction.Target, query: String): MusicBrainzQuery =
            MusicBrainzQuery(context, this@WebSearchViewModel, target, query)

        fun musicBrainzView(context: Context, target: MusicBrainzAction.Target, mbid: String): MusicBrainzQuery =
            musicBrainzQuery(context).also {
                it.query(context, MusicBrainzAction.View(target, mbid))
            }
    }
}
