/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.coil.album

import coil.key.Keyer
import coil.request.Options
import player.phonograph.mechanism.coil.model.AlbumImage

class AlbumImageKeyer : Keyer<AlbumImage> {
    override fun key(data: AlbumImage, options: Options): String =
        with(data) { "$name($id)@${options.size}" }
}