/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.web

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import kotlinx.coroutines.flow.MutableStateFlow

class LastFmQuery(
    releaseQuery: String? = null,
    artistQuery: String? = null,
    trackQuery: String? = null,
    target: Target = Target.Release,
) : Query("Last.fm") {

    enum class Target {
        Artist,
        Release,
        Track,
        ;
    }

    val target: MutableStateFlow<Target> = MutableStateFlow(target)

    val releaseQuery: MutableStateFlow<String?> = MutableStateFlow(releaseQuery)
    val artistQuery: MutableStateFlow<String?> = MutableStateFlow(artistQuery)
    val trackQuery: MutableStateFlow<String?> = MutableStateFlow(trackQuery)

    fun action(): QueryAction {
        return when (target.value) {
            Target.Artist  -> QueryAction.Artist(artistQuery.value.orEmpty())
            Target.Release -> QueryAction.Release(releaseQuery.value.orEmpty())
            Target.Track   -> QueryAction.Track(trackQuery.value.orEmpty(), artistQuery.value.orEmpty())
        }
    }

    sealed class QueryAction {
        data class Artist(val name: String) : QueryAction()
        data class Release(val name: String) : QueryAction()
        data class Track(val name: String, val artist: String?) : QueryAction()
    }

    companion object {
        fun from(artist: Artist): LastFmQuery =
            LastFmQuery(
                artistQuery = artist.name,
                target = Target.Artist
            )

        fun from(album: Album): LastFmQuery =
            LastFmQuery(
                releaseQuery = album.title,
                artistQuery = album.artistName,
                target = Target.Release
            )

        fun from(song: Song): LastFmQuery =
            LastFmQuery(
                releaseQuery = song.albumName,
                artistQuery = song.artistName,
                trackQuery = song.title,
                target = Target.Track
            )
    }
}