/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.lastfm

import util.phonograph.tagsources.lastfm.AlbumResult
import util.phonograph.tagsources.lastfm.ArtistResult
import util.phonograph.tagsources.lastfm.LastFmAlbum
import util.phonograph.tagsources.lastfm.LastFmArtist
import util.phonograph.tagsources.lastfm.LastFmImage

data class LastFmImageBundle(
    val owner: String,
    val images: List<LastFmImage>,
) {
    companion object {
        fun from(artist: LastFmArtist) = LastFmImageBundle("${artist.name}:${artist.url}", artist.image)
        fun from(album: LastFmAlbum) = LastFmImageBundle("${album.name}:${album.url}", album.image)
        fun from(artist: ArtistResult.Artist) = LastFmImageBundle("${artist.name}:${artist.url}", artist.image)
        fun from(album: AlbumResult.Album) = LastFmImageBundle("${album.name}:${album.url}", album.image)
    }
}
