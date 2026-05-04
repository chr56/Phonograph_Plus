/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.coil.album

import coil.map.Mapper
import coil.request.Options
import player.phonograph.mechanism.coil.model.AlbumImage
import player.phonograph.model.Album

class AlbumImageMapper : Mapper<Album, AlbumImage> {
    override fun map(data: Album, options: Options): AlbumImage =
        AlbumImage(data.id, data.title)
}