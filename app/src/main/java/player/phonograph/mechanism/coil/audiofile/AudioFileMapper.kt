/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.mechanism.coil.audiofile

import coil.map.Mapper
import coil.request.Options
import player.phonograph.mechanism.coil.model.SongImage
import player.phonograph.model.Song
import java.io.File

class AudioFileMapper : Mapper<Song, SongImage> {
    override fun map(data: Song, options: Options): SongImage? {
        val available = runCatching { File(data.data).exists() }.getOrElse { false } // if file is  available
        return if (available) {
            SongImage.Companion.from(data)
        } else {
            null
        }
    }
}
