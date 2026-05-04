/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.coil.audiofile

import coil.key.Keyer
import coil.request.Options
import player.phonograph.mechanism.coil.model.SongImage

class AudioFileKeyer : Keyer<SongImage> {
    override fun key(data: SongImage, options: Options): String {
        return "${data.songId}@${options.size}"
    }
}
