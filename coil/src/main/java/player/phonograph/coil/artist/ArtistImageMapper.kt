/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.artist

import coil.map.Mapper
import coil.request.Options
import player.phonograph.model.Artist

class ArtistImageMapper : Mapper<Artist, ArtistImage> {
    override fun map(data: Artist, options: Options): ArtistImage? =
        ArtistImage(
            data.name,
            data.id,
            data.albums.map { ArtistImage.AlbumCover(it.id, it.year, it.safeGetFirstSong().data) }
        ).takeIf { it.albumCovers.isNotEmpty() }
}
