/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.lastfm

import mms.lastfm.AlbumResult
import mms.lastfm.ArtistResult
import mms.lastfm.LastFmAlbum
import mms.lastfm.LastFmArtist
import mms.lastfm.LastFmImage

data class LastFmImageBundle(
    val owner: String,
    val images: List<LastFmImage>,
    val preferredSize: LastFmImage.ImageSize?,
) {
    companion object {
        fun from(artist: LastFmArtist, preferredSize: LastFmImage.ImageSize? = null) =
            LastFmImageBundle("${artist.name}:${artist.url}", artist.image, preferredSize)

        fun from(album: LastFmAlbum, preferredSize: LastFmImage.ImageSize? = null) =
            LastFmImageBundle("${album.name}:${album.url}", album.image, preferredSize)

        fun from(artist: ArtistResult.Artist, preferredSize: LastFmImage.ImageSize? = null) =
            LastFmImageBundle("${artist.name}:${artist.url}", artist.image, preferredSize)

        fun from(album: AlbumResult.Album, preferredSize: LastFmImage.ImageSize? = null) =
            LastFmImageBundle("${album.name}:${album.url}", album.image, preferredSize)
    }
}
