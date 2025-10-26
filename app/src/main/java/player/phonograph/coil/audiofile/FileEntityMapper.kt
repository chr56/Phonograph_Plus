/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.audiofile

import coil.map.Mapper
import coil.request.Options
import player.phonograph.coil.model.SongImage
import player.phonograph.model.file.FileItem

class FileEntityMapper : Mapper<FileItem, SongImage> {
    override fun map(data: FileItem, options: Options): SongImage? {
        return if (data.content is FileItem.SongContent) SongImage.from(data.content.song) else null
    }
}
