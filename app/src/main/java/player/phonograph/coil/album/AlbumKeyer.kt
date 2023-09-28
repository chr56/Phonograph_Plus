/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.album

import coil.key.Keyer
import coil.request.Options
import player.phonograph.model.Album

class AlbumKeyer : Keyer<Album> {
    override fun key(data: Album, options: Options): String {
        return with(data) { "$title($id)@${options.size}" }
    }
}
