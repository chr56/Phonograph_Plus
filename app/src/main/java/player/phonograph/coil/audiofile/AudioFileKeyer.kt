/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.audiofile

import coil.key.Keyer
import coil.request.Options

class AudioFileKeyer : Keyer<AudioFile> {
    override fun key(data: AudioFile, options: Options): String {
        return "${data.songId}@${options.size}"
    }
}
