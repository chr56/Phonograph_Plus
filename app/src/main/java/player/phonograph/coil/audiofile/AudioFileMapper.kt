/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.audiofile

import coil.map.Mapper
import coil.request.Options
import player.phonograph.coil.model.SongImage
import java.io.File
import player.phonograph.model.Song

class AudioFileMapper : Mapper<Song, SongImage> {
    override fun map(data: Song, options: Options): SongImage? {
        val available = runCatching { File(data.data).exists() }.getOrElse { false } // if file is  available
        return if (available) {
            SongImage.from(data)
        } else {
            null
        }
    }
}
