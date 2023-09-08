/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.ui.compose.base.Navigator
import androidx.lifecycle.ViewModel
import android.content.Context

class WebSearchViewModel : ViewModel() {

    val navigator = Navigator<Page>(Page.Home)

    val queryFactory = QueryFactory()

    @Suppress("DeferredResultUnused")
    inner class QueryFactory {

        fun lastFmQuery(context: Context): LastFmQuery = LastFmQuery(context, this@WebSearchViewModel)

        fun lastFmQuery(context: Context, artist: Artist): LastFmQuery =
            LastFmQuery(
                context, this@WebSearchViewModel,
                artistQuery = artist.name,
                target = LastFmQuery.Target.Artist
            )

        fun lastFmQuery(context: Context, album: Album): LastFmQuery =
            LastFmQuery(
                context, this@WebSearchViewModel,
                releaseQuery = album.title,
                artistQuery = album.artistName,
                target = LastFmQuery.Target.Release
            )

        fun lastFmQuery(context: Context, song: Song): LastFmQuery =
            LastFmQuery(
                context, this@WebSearchViewModel,
                releaseQuery = song.albumName,
                artistQuery = song.artistName,
                trackQuery = song.title,
                target = LastFmQuery.Target.Track
            )


        fun musicBrainzQuery(context: Context): MusicBrainzQuery =
            MusicBrainzQuery(context, this@WebSearchViewModel, MusicBrainzQuery.Target.Release, "")

        fun musicBrainzQuery(context: Context, target: MusicBrainzQuery.Target, query: String): MusicBrainzQuery =
            MusicBrainzQuery(context, this@WebSearchViewModel, target, query)

        fun musicBrainzView(context: Context, target: MusicBrainzQuery.Target, mbid: String): MusicBrainzQuery =
            musicBrainzQuery(context).also {
                it.query(
                    context,
                    when (target) {
                        MusicBrainzQuery.Target.ReleaseGroup ->
                            MusicBrainzQuery.QueryAction.ViewReleaseGroup(mbid)

                        MusicBrainzQuery.Target.Release      ->
                            MusicBrainzQuery.QueryAction.ViewRelease(mbid)

                        MusicBrainzQuery.Target.Artist       ->
                            MusicBrainzQuery.QueryAction.ViewArtist(mbid)

                        MusicBrainzQuery.Target.Recording    ->
                            MusicBrainzQuery.QueryAction.ViewRecording(mbid)
                    }
                )
            }
    }
}
