/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil.album

import coil.key.Keyer
import coil.request.Options
import player.phonograph.coil.model.AlbumImage

class AlbumImageKeyer : Keyer<AlbumImage> {
    override fun key(data: AlbumImage, options: Options): String =
        with(data) { "$name($id)@${options.size}" }
}