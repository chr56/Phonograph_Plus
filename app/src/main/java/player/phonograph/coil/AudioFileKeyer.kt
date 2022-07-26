/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.coil

import coil.key.Keyer
import coil.request.Options

class AudioFileKeyer : Keyer<AudioFile> {
    override fun key(data: AudioFile, options: Options): String {
        return data.songId.toString()
    }
}
