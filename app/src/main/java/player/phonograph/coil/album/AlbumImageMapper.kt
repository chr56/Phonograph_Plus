package player.phonograph.coil.album

import coil.map.Mapper
import coil.request.Options
import player.phonograph.coil.audiofile.AudioFile
import player.phonograph.model.Album
import player.phonograph.repo.loader.Songs

class AlbumImageMapper : Mapper<Album, AlbumImage> {
    override fun map(data: Album, options: Options): AlbumImage? = AlbumImage(
        data.title,
        data.id,
        Songs.album(options.context, data.id).map { AudioFile.from(it) }
    ).takeIf { it.files.isNotEmpty() }
}