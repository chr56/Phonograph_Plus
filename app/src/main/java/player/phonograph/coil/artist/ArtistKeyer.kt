/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.artist

import coil.key.Keyer
import coil.request.Options
import player.phonograph.model.Artist

class ArtistKeyer : Keyer<Artist> {
    override fun key(data: Artist, options: Options): String {
        return with(data) { "$name($id)@${options.size}" }
    }
}
