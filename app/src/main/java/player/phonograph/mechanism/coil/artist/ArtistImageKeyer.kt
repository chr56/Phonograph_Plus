/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.coil.artist

import coil.key.Keyer
import coil.request.Options
import player.phonograph.mechanism.coil.model.ArtistImage

class ArtistImageKeyer : Keyer<ArtistImage> {
    override fun key(data: ArtistImage, options: Options): String =
        with(data) { "$name($id)@${options.size}" }
}
