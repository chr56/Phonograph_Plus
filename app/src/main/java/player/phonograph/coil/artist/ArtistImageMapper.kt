/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.artist

import coil.map.Mapper
import coil.request.Options
import player.phonograph.model.Artist
import player.phonograph.repo.mediastore.loaders.ArtistSongLoader.allSongs

class ArtistImageMapper : Mapper<Artist, ArtistImage> {
    override fun map(data: Artist, options: Options): ArtistImage? =
        ArtistImage(
            data.name,
            data.id,
            data.allSongs(options.context).map { ArtistImage.SongCover(it.id, it.year, it.data) }
        ).takeIf { it.songCovers.isNotEmpty() }
}
