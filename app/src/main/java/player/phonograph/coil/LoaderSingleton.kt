/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import android.content.Context
import coil.ImageLoader
import player.phonograph.coil.audiofile.AudioFileFetcher
import player.phonograph.coil.audiofile.AudioFileKeyer
import player.phonograph.coil.audiofile.AudioFileMapper

fun createPhonographImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .allowHardware(false)
        .components {
            add(AudioFileKeyer())
            add(AudioFileMapper())
            add(AudioFileFetcher.Factory())
        }
        .crossfade(true)
        .build()
}
