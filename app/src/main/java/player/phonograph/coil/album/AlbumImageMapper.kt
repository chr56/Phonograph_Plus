package player.phonograph.coil.album

import coil.map.Mapper
import coil.request.Options
import player.phonograph.coil.model.AlbumImage
import player.phonograph.model.Album

class AlbumImageMapper : Mapper<Album, AlbumImage> {
    override fun map(data: Album, options: Options): AlbumImage =
        AlbumImage(data.id, data.title)
}