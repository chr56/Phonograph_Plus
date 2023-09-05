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

    inner class QueryFactory {

        fun lastFm(context: Context): LastFmQuery = LastFmQuery(context, this@WebSearchViewModel)

        fun from(context: Context, artist: Artist): LastFmQuery =
            LastFmQuery(
                context, this@WebSearchViewModel,
                artistQuery = artist.name,
                target = LastFmQuery.Target.Artist
            )

        fun from(context: Context, album: Album): LastFmQuery =
            LastFmQuery(
                context, this@WebSearchViewModel,
                releaseQuery = album.title,
                artistQuery = album.artistName,
                target = LastFmQuery.Target.Release
            )

        fun from(context: Context, song: Song): LastFmQuery =
            LastFmQuery(
                context, this@WebSearchViewModel,
                releaseQuery = song.albumName,
                artistQuery = song.artistName,
                trackQuery = song.title,
                target = LastFmQuery.Target.Track
            )

        fun musicBrainzQuery(context: Context): MusicBrainzQuery =
            MusicBrainzQuery(context, this@WebSearchViewModel)

        fun musicBrainzQueryReleaseGroup(context: Context, mbid: String): MusicBrainzQuery =
            musicBrainzQuery(context).also { it.query(context, MusicBrainzQuery.QueryAction.ViewReleaseGroup(mbid)) }

        fun musicBrainzQueryRelease(context: Context, mbid: String): MusicBrainzQuery =
            musicBrainzQuery(context).also { it.query(context, MusicBrainzQuery.QueryAction.ViewRelease(mbid)) }

        fun musicBrainzQueryArtist(context: Context, mbid: String): MusicBrainzQuery =
            musicBrainzQuery(context).also { it.query(context, MusicBrainzQuery.QueryAction.ViewArtist(mbid)) }

        fun musicBrainzQueryRecording(context: Context, mbid: String): MusicBrainzQuery =
            musicBrainzQuery(context).also { it.query(context, MusicBrainzQuery.QueryAction.ViewRecording(mbid)) }
    }
}
