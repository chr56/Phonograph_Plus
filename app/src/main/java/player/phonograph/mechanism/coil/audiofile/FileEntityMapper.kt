/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.coil.audiofile

import coil.map.Mapper
import coil.request.Options
import player.phonograph.mechanism.coil.model.SongImage
import player.phonograph.model.file.FileItem

class FileEntityMapper : Mapper<FileItem, SongImage> {
    override fun map(data: FileItem, options: Options): SongImage? {
        return if (data.content is FileItem.SongContent) SongImage.Companion.from(data.content.song) else null
    }
}
