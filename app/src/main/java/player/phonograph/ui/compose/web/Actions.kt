/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import util.phonograph.tagsources.lastfm.AlbumResult
import util.phonograph.tagsources.lastfm.ArtistResult
import util.phonograph.tagsources.lastfm.TrackResult
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface WebSearchAction

sealed interface MusicBrainzAction : WebSearchAction {

    enum class Target(val displayName: String, val urlName: String) {
        ReleaseGroup("Release Group", "release-group"),
        Release("Release", "release"),
        Artist("Artist", "artist"),
        Recording("Recording", "recording"),
        ;

        fun link(mbid: String): String = "https://musicbrainz.org/${urlName}/$mbid"
    }

    @Parcelize
    data class Search(val target: Target, val query: String) : MusicBrainzAction, Parcelable
    @Parcelize
    data class View(val target: Target, val mbid: String) : MusicBrainzAction, Parcelable
}

sealed interface LastFmAction : WebSearchAction {

    enum class Target(val displayName: String) {
        Artist("Artist"),
        Album("Album"),
        Track("Track"),
        ;
    }

    @Parcelize
    data class Search(
        val target: Target,
        val album: String = "",
        val artist: String = "",
        val track: String = "",
    ) : LastFmAction, Parcelable

    sealed interface View : LastFmAction {
        data class ViewArtist(val item: ArtistResult.Artist) : View
        data class ViewAlbum(val item: AlbumResult.Album) : View
        data class ViewTrack(val item: TrackResult.Track) : View
    }
}



sealed class Source(val name: String) {
    object MusicBrainz : Source("MusicBrainz")
    object LastFm : Source("last.fm")
}

object WebSearchActionConst {
    const val MUSICBRAINZ_SEARCH = "musicbrainz_search"
    const val MUSICBRAINZ_VIEW = "musicbrainz_view"
    const val LASTFM_SEARCH = "lastfm_search"
    const val LASTFM_VIEW_ARTIST = "lastfm_view_artist"
    const val LASTFM_VIEW_ALBUM = "lastfm_view_album"
    const val LASTFM_VIEW_TRACK = "lastfm_view_track"
}