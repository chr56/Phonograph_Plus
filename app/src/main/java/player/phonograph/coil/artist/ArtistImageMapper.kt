/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.artist

import coil.map.Mapper
import coil.request.Options
import player.phonograph.coil.audiofile.AudioFile
import player.phonograph.model.Artist
import player.phonograph.repo.loader.Songs

class ArtistImageMapper : Mapper<Artist, ArtistImage> {
    override fun map(data: Artist, options: Options): ArtistImage? =
        ArtistImage(
            data.name,
            data.id,
            Songs.artist(options.context, data.id).map { AudioFile.from(it) }
        ).takeIf { it.files.isNotEmpty() }
}
