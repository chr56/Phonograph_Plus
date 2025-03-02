/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil.artist

import coil.key.Keyer
import coil.request.Options
import player.phonograph.coil.model.ArtistImage

class ArtistImageKeyer : Keyer<ArtistImage> {
    override fun key(data: ArtistImage, options: Options): String =
        with(data) { "$name($id)@${options.size}" }
}
