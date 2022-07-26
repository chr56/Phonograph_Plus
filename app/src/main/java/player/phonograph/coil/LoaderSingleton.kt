/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import android.content.Context
import coil.ImageLoader

fun createPhonographImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .allowHardware(false)
        .components {
            add(AudioFileMapper())
            add(AudioFileFetcher.Factory())
        }
        .crossfade(true)
        .build()
}
