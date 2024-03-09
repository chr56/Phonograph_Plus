/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.artist

import coil.map.Mapper
import coil.request.Options
import player.phonograph.coil.model.ArtistImage
import player.phonograph.coil.model.SongImage
import player.phonograph.model.Artist
import player.phonograph.repo.loader.Songs
import kotlinx.coroutines.runBlocking

class ArtistImageMapper : Mapper<Artist, ArtistImage> {
    override fun map(data: Artist, options: Options): ArtistImage? =
        ArtistImage(
            data.id,
            data.name,
            runBlocking { Songs.artist(options.context, data.id).map { SongImage.from(it) } }
        ).takeIf { it.files.isNotEmpty() }
}
