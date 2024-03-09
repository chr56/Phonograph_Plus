package player.phonograph.coil.album

import coil.map.Mapper
import coil.request.Options
import player.phonograph.coil.model.SongImage
import player.phonograph.coil.model.AlbumImage
import player.phonograph.model.Album
import player.phonograph.repo.loader.Songs
import kotlinx.coroutines.runBlocking

class AlbumImageMapper : Mapper<Album, AlbumImage> {
    override fun map(data: Album, options: Options): AlbumImage? = AlbumImage(
        data.id,
        data.title,
        runBlocking{ Songs.album(options.context, data.id).map { SongImage.from(it) } }
    ).takeIf { it.files.isNotEmpty() }
}