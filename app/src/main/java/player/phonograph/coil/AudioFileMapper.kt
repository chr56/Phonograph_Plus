/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import coil.map.Mapper
import coil.request.Options
import java.io.File
import player.phonograph.model.Song

class AudioFileMapper : Mapper<Song, AudioFile> {
    override fun map(data: Song, options: Options): AudioFile? {
        val available = runCatching { File(data.data).exists() }.getOrElse { false } // if file is  available
        return if (available) {
            AudioFile.from(data)
        } else {
            null
        }
    }
}
